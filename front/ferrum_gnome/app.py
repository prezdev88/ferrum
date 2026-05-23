from __future__ import annotations

import threading
import urllib.parse
import webbrowser
from pathlib import Path

import gi
import requests

gi.require_version("Gdk", "4.0")
gi.require_version("GdkPixbuf", "2.0")
gi.require_version("Graphene", "1.0")
gi.require_version("Gtk", "4.0")
gi.require_version("Adw", "1")
gi.require_version("Pango", "1.0")

from gi.repository import Adw, Gdk, GdkPixbuf, Gio, GLib, Graphene, Gtk, Pango

from .backend import BackendError, FerrumBackend
from .models import AlbumDetail, AlbumEntry, BandDetail, BandSummary, SearchHistoryEntry, TrackEntry
from .settings import SettingsStore, UserSettings, generate_random_color, normalize_hex_color

APP_ID = "io.github.prezdev.Ferrum"
THEME_MODES = [
    ("System", "system"),
    ("Light", "light"),
    ("Dark", "dark"),
    ("Black", "black"),
]

SEARCH_TYPES = [
    ("Band name", "BAND_NAME"),
    ("Music genre", "MUSIC_GENRE"),
    ("Themes", "THEMES"),
    ("Album title", "ALBUM_TITLE"),
    ("Song title", "SONG_TITLE"),
    ("Label", "LABEL"),
    ("Artist", "ARTIST"),
    ("User profile", "USER_PROFILE"),
    ("Google", "GOOGLE"),
]

PROVIDERS = [
    ("YouTube Music", "youtube_music"),
    ("YouTube", "youtube"),
]


def _append_classes(widget: Gtk.Widget, *classes: str) -> Gtk.Widget:
    for css_class in classes:
        widget.add_css_class(css_class)
    return widget


def _normalize_album_type(album_type: str) -> str:
    normalized_type = (album_type or "").strip().lower()
    slug_characters = [
        character if character.isalnum() else "-"
        for character in normalized_type
    ]
    slug = "".join(slug_characters).strip("-")
    while "--" in slug:
        slug = slug.replace("--", "-")
    return slug or "other"


def _album_type_css_class(album_type: str) -> str:
    return f"album-type-{_normalize_album_type(album_type)}"


def _resolve_album_type_name(album_type: str) -> str:
    return (album_type or "").strip() or "Other"


def _hex_to_rgb(color: str) -> tuple[int, int, int]:
    normalized_color = color.lstrip("#")
    return (
        int(normalized_color[0:2], 16),
        int(normalized_color[2:4], 16),
        int(normalized_color[4:6], 16),
    )


def _mix_color(color: str, target_color: str, ratio: float) -> str:
    source_red, source_green, source_blue = _hex_to_rgb(color)
    target_red, target_green, target_blue = _hex_to_rgb(target_color)
    mixed_red = round(source_red * (1 - ratio) + target_red * ratio)
    mixed_green = round(source_green * (1 - ratio) + target_green * ratio)
    mixed_blue = round(source_blue * (1 - ratio) + target_blue * ratio)
    return "#{:02X}{:02X}{:02X}".format(mixed_red, mixed_green, mixed_blue)


def _rgba(color: str, alpha: float) -> str:
    red, green, blue = _hex_to_rgb(color)
    return f"rgba({red}, {green}, {blue}, {alpha:.2f})"


class AlbumDialog(Adw.Dialog):
    def __init__(
        self,
        app: Adw.Application,
        album: AlbumDetail,
        band_name: str,
        music_provider: str,
    ) -> None:
        super().__init__()
        self.album = album
        self.band_name = band_name
        self.music_provider = music_provider
        self.set_title(album.title)
        self.set_content_width(820)
        self.set_content_height(700)
        self.set_follows_content_size(False)
        self.set_can_close(True)
        self.set_presentation_mode(Adw.DialogPresentationMode.FLOATING)
        backdrop_click = Gtk.GestureClick.new()
        backdrop_click.connect("pressed", self.on_backdrop_pressed)
        self.add_controller(backdrop_click)

        toolbar = Adw.ToolbarView()
        toolbar.add_css_class("album-dialog-shell")
        toolbar.add_css_class(app.resolve_window_theme_class(app.settings.theme_mode))
        header = Adw.HeaderBar()
        toolbar.add_top_bar(header)

        content = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=18)
        content.set_margin_top(24)
        content.set_margin_bottom(24)
        content.set_margin_start(24)
        content.set_margin_end(24)

        hero = Gtk.Box(orientation=Gtk.Orientation.HORIZONTAL, spacing=18)
        hero.add_css_class("card")
        hero.add_css_class("hero-card")
        hero.set_margin_bottom(6)

        hero_content = Gtk.Box(orientation=Gtk.Orientation.HORIZONTAL, spacing=18)
        hero_content.set_halign(Gtk.Align.START)
        hero_content.set_hexpand(False)

        hero_content.append(
            self.build_remote_artwork(
                album.image_url,
                132,
                132,
                "album-artwork",
                Gtk.ContentFit.COVER,
                True,
            )
        )

        hero_text = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=8)
        hero_text.set_hexpand(True)
        hero_text.set_halign(Gtk.Align.START)

        title = Gtk.Label(label=album.title, xalign=0)
        title.set_wrap(True)
        title.add_css_class("title-2")
        title.set_halign(Gtk.Align.START)

        subtitle = Gtk.Label(
            label="  •  ".join(filter(None, [album.type, album.release_date, album.label])),
            xalign=0,
        )
        subtitle.set_wrap(True)
        subtitle.add_css_class("dim-label")
        subtitle.set_halign(Gtk.Align.START)

        actions = Gtk.Box(orientation=Gtk.Orientation.HORIZONTAL, spacing=8)
        actions.set_halign(Gtk.Align.START)
        open_album = Gtk.Button(label="Open in Metal Archives")
        open_album.add_css_class("pill")
        open_album.connect("clicked", lambda *_: webbrowser.open(album.url))
        actions.append(open_album)

        hero_text.append(title)
        hero_text.append(subtitle)
        hero_text.append(actions)
        hero_content.append(hero_text)
        hero.append(hero_content)

        tracks_box = Gtk.ListBox()
        tracks_box.add_css_class("boxed-list")
        tracks_box.set_selection_mode(Gtk.SelectionMode.NONE)

        for track in album.tracks:
            tracks_box.append(self._build_track_row(track))

        content.append(hero)
        content.append(_append_classes(Gtk.Label(label="Tracklist", xalign=0), "heading"))
        content.append(tracks_box)

        scroller = Gtk.ScrolledWindow()
        scroller.set_policy(Gtk.PolicyType.NEVER, Gtk.PolicyType.AUTOMATIC)
        scroller.set_child(content)

        toolbar.set_content(scroller)
        self.set_child(toolbar)

    def on_backdrop_pressed(
        self,
        _gesture: Gtk.GestureClick,
        _n_press: int,
        click_x: float,
        click_y: float,
    ) -> None:
        content = self.get_child()
        if content is None:
            self.close()
            return

        inside_bounds, bounds = content.compute_bounds(self)
        if not inside_bounds:
            return

        if self.is_point_inside_rect(click_x, click_y, bounds):
            return

        self.close()

    def is_point_inside_rect(self, point_x: float, point_y: float, rect: Graphene.Rect) -> bool:
        rect_x = rect.get_x()
        rect_y = rect.get_y()
        rect_width = rect.get_width()
        rect_height = rect.get_height()
        return (
            rect_x <= point_x <= rect_x + rect_width
            and rect_y <= point_y <= rect_y + rect_height
        )

    def _build_track_row(self, track: TrackEntry) -> Gtk.ListBoxRow:
        row = Gtk.ListBoxRow()
        box = Gtk.Box(orientation=Gtk.Orientation.HORIZONTAL, spacing=12)
        box.set_margin_top(10)
        box.set_margin_bottom(10)
        box.set_margin_start(12)
        box.set_margin_end(12)

        left = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=4)
        left.set_hexpand(True)

        heading = Gtk.Label(
            label=f"{track.number}. {track.title}" if track.number else track.title,
            xalign=0,
        )
        heading.set_wrap(True)
        heading.add_css_class("track-title")

        duration = Gtk.Label(label=track.duration or "Unknown length", xalign=0)
        duration.add_css_class("dim-label")

        left.append(heading)
        left.append(duration)

        button = Gtk.Button(icon_name="media-playback-start-symbolic")
        button.add_css_class("circular")
        button.set_tooltip_text("Search track in music provider")
        button.connect("clicked", lambda *_: self._open_track(track))

        box.append(left)
        box.append(button)
        row.set_child(box)
        return row

    def _open_track(self, track: TrackEntry) -> None:
        query = " ".join(value for value in [self.band_name, track.title, self.album.title] if value)
        encoded = urllib.parse.quote(query)
        if self.music_provider == "youtube":
            url = f"https://www.youtube.com/results?search_query={encoded}"
        else:
            url = f"https://music.youtube.com/search?q={encoded}"
        webbrowser.open(url)

    def build_remote_artwork(
        self,
        image_url: str,
        width: int,
        height: int,
        css_class: str,
        content_fit: Gtk.ContentFit = Gtk.ContentFit.CONTAIN,
        square_crop: bool = False,
    ) -> Gtk.Widget:
        frame = Gtk.Frame()
        frame.add_css_class("artwork-frame")
        frame.add_css_class(css_class)
        frame.set_size_request(width, height)
        frame.set_hexpand(False)
        frame.set_vexpand(False)
        frame.set_halign(Gtk.Align.START)
        frame.set_valign(Gtk.Align.START)
        frame.set_overflow(Gtk.Overflow.HIDDEN)

        stack = Gtk.Stack()
        stack.set_size_request(width, height)
        stack.set_overflow(Gtk.Overflow.HIDDEN)

        placeholder = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=8)
        placeholder.set_size_request(width, height)
        placeholder.set_halign(Gtk.Align.CENTER)
        placeholder.set_valign(Gtk.Align.CENTER)
        placeholder.add_css_class("artwork-placeholder")
        placeholder.set_overflow(Gtk.Overflow.HIDDEN)

        icon = Gtk.Image.new_from_icon_name("image-x-generic-symbolic")
        icon.set_pixel_size(max(32, min(width, height) // 4))
        placeholder.append(icon)

        picture = Gtk.Picture()
        picture.set_can_shrink(True)
        picture.set_content_fit(content_fit)
        picture.set_keep_aspect_ratio(True)
        picture.set_size_request(width, height)
        picture.set_hexpand(False)
        picture.set_vexpand(False)
        picture.set_overflow(Gtk.Overflow.HIDDEN)

        stack.add_named(placeholder, "placeholder")
        stack.add_named(picture, "image")
        stack.set_visible_child_name("placeholder")
        frame.set_child(stack)

        if image_url:
            self.load_remote_artwork(image_url, picture, stack, width, height, square_crop)

        return frame

    def load_remote_artwork(
        self,
        image_url: str,
        picture: Gtk.Picture,
        stack: Gtk.Stack,
        width: int,
        height: int,
        square_crop: bool,
    ) -> None:
        def runner() -> None:
            try:
                response = requests.get(image_url, timeout=30)
                response.raise_for_status()
                texture = self.build_texture(response.content, width, height, square_crop)
            except Exception:
                return

            GLib.idle_add(self.apply_remote_artwork, picture, stack, texture)

        threading.Thread(target=runner, daemon=True).start()

    def build_texture(
        self,
        image_bytes: bytes,
        width: int,
        height: int,
        square_crop: bool,
    ) -> Gdk.Texture:
        if not square_crop:
            return Gdk.Texture.new_from_bytes(GLib.Bytes.new(image_bytes))

        loader = GdkPixbuf.PixbufLoader()
        loader.write(image_bytes)
        loader.close()
        pixbuf = loader.get_pixbuf()
        if pixbuf is None:
            raise ValueError("Could not decode artwork")

        if square_crop:
            pixbuf = self.crop_center_square(pixbuf)

        scaled_pixbuf = pixbuf.scale_simple(width, height, GdkPixbuf.InterpType.BILINEAR)
        if scaled_pixbuf is None:
            raise ValueError("Could not scale artwork")
        return Gdk.Texture.new_for_pixbuf(scaled_pixbuf)

    def crop_center_square(self, pixbuf: GdkPixbuf.Pixbuf) -> GdkPixbuf.Pixbuf:
        crop_size = min(pixbuf.get_width(), pixbuf.get_height())
        offset_x = (pixbuf.get_width() - crop_size) // 2
        offset_y = (pixbuf.get_height() - crop_size) // 2
        return pixbuf.new_subpixbuf(offset_x, offset_y, crop_size, crop_size)

    def apply_remote_artwork(
        self,
        picture: Gtk.Picture,
        stack: Gtk.Stack,
        texture: Gdk.Texture,
    ) -> bool:
        picture.set_paintable(texture)
        stack.set_visible_child_name("image")
        return False


class LoadingDialog(Adw.Dialog):
    def __init__(self, app: Adw.Application, title: str, message: str) -> None:
        super().__init__()
        self.set_title(title)
        self.set_content_width(360)
        self.set_content_height(180)
        self.set_follows_content_size(False)
        self.set_can_close(False)
        self.set_presentation_mode(Adw.DialogPresentationMode.FLOATING)

        content = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=16)
        content.set_margin_top(24)
        content.set_margin_bottom(24)
        content.set_margin_start(24)
        content.set_margin_end(24)
        content.set_valign(Gtk.Align.CENTER)
        content.set_halign(Gtk.Align.CENTER)

        spinner = Gtk.Spinner()
        spinner.set_spinning(True)
        spinner.set_size_request(36, 36)

        title_label = Gtk.Label(label=title, xalign=0.5)
        title_label.add_css_class("title-3")

        message_label = Gtk.Label(label=message, xalign=0.5)
        message_label.add_css_class("dim-label")
        message_label.set_wrap(True)
        message_label.set_justify(Gtk.Justification.CENTER)

        content.append(spinner)
        content.append(title_label)
        content.append(message_label)

        shell = Gtk.Box(orientation=Gtk.Orientation.VERTICAL)
        shell.add_css_class(app.resolve_window_theme_class(app.settings.theme_mode))
        shell.append(content)
        self.set_child(shell)


class FerrumWindow(Adw.ApplicationWindow):
    def __init__(self, app: Adw.Application) -> None:
        super().__init__(application=app, title="Ferrum", default_width=1320, default_height=860)
        self.app = app
        self.backend = FerrumBackend()
        self.results: list[BandSummary] = []
        self.search_history: list[SearchHistoryEntry] = []
        self.selected_band: BandDetail | None = None
        self.discography_list: Gtk.ListBox | None = None
        self.last_submitted_query = ""
        self.last_submitted_search_type = SEARCH_TYPES[0][1]
        self.search_history_dialog: Adw.Dialog | None = None
        self.loading_dialog: LoadingDialog | None = None
        self.discography_type_filter_values: list[str | None] = [None]
        self.discography_type_filter_dropdown: Gtk.DropDown | None = None

        self.toast_overlay = Adw.ToastOverlay()
        toolbar = Adw.ToolbarView()
        header = Adw.HeaderBar()

        title_widget = Adw.WindowTitle(title="Ferrum", subtitle="Metal Archives in a Linux-native shell")
        header.set_title_widget(title_widget)
        header.pack_end(self.app.build_settings_button(self))
        toolbar.add_top_bar(header)

        main_box = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=18)
        main_box.set_margin_top(18)
        main_box.set_margin_bottom(18)
        main_box.set_margin_start(18)
        main_box.set_margin_end(18)

        main_box.append(self._build_search_surface())
        main_box.append(self._build_split_view())

        toolbar.set_content(main_box)
        self.toast_overlay.set_child(toolbar)
        self.set_content(self.toast_overlay)
        self.app.apply_theme()
        self.load_search_history()

    def _build_search_surface(self) -> Gtk.Widget:
        surface = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=10)
        surface.add_css_class("card")
        surface.add_css_class("search-surface")

        controls = Gtk.Box(orientation=Gtk.Orientation.HORIZONTAL, spacing=10)

        self.query_entry = Gtk.SearchEntry()
        self.query_entry.set_hexpand(True)
        self.query_entry.set_placeholder_text("Try: Bathory, funeral doom, Chile, cosmic themes…")
        self.query_entry.connect("activate", lambda *_: self.on_search())

        self.search_type_dropdown = Gtk.DropDown.new_from_strings([label for label, _ in SEARCH_TYPES])
        self.search_type_dropdown.set_selected(0)

        self.search_history_button = Gtk.Button(label="View search history")
        self.search_history_button.connect("clicked", lambda *_: self.present_search_history_dialog())

        self.search_button = Gtk.Button(label="Search")
        self.search_button.add_css_class("suggested-action")
        self.search_button.connect("clicked", lambda *_: self.on_search())

        controls.append(self.query_entry)
        controls.append(self.search_type_dropdown)
        controls.append(self.search_history_button)
        controls.append(self.search_button)

        surface.append(controls)
        return surface

    def _build_split_view(self) -> Gtk.Widget:
        pane = Gtk.Paned.new(Gtk.Orientation.HORIZONTAL)
        pane.set_wide_handle(True)
        pane.set_position(420)

        pane.set_start_child(self._build_results_panel())
        pane.set_end_child(self._build_detail_panel())
        return pane

    def _build_results_panel(self) -> Gtk.Widget:
        wrapper = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=12)
        wrapper.add_css_class("card")

        heading = Gtk.Box(orientation=Gtk.Orientation.HORIZONTAL, spacing=12)
        heading_label = Gtk.Label(label="Results", xalign=0)
        heading_label.add_css_class("title-4")
        heading_label.set_hexpand(True)

        self.results_count = Gtk.Label(label="No results yet", xalign=1)
        self.results_count.add_css_class("dim-label")

        heading.append(heading_label)
        heading.append(self.results_count)

        self.results_stack = Gtk.Stack()
        self.results_stack.set_vexpand(True)

        empty = Adw.StatusPage(
            title="Start with a band, genre or theme",
            description="Search results will appear here and load band details on selection.",
            icon_name="system-search-symbolic",
        )

        self.results_list = Gtk.ListBox()
        self.results_list.add_css_class("boxed-list")
        self.results_list.set_selection_mode(Gtk.SelectionMode.SINGLE)
        self.results_list.connect("row-activated", self.on_result_activated)

        scroller = Gtk.ScrolledWindow()
        scroller.set_policy(Gtk.PolicyType.NEVER, Gtk.PolicyType.AUTOMATIC)
        scroller.set_child(self.results_list)

        self.results_stack.add_named(empty, "empty")
        self.results_stack.add_named(scroller, "list")
        self.results_stack.set_visible_child_name("empty")

        wrapper.append(heading)
        wrapper.append(self.results_stack)
        return wrapper

    def _build_detail_panel(self) -> Gtk.Widget:
        self.detail_stack = Gtk.Stack()
        self.detail_stack.set_vexpand(True)

        placeholder = Adw.StatusPage(
            title="Band details live here",
            description="Pick a result to inspect line-up context, metadata and discography.",
            icon_name="audio-x-generic-symbolic",
        )

        self.detail_content = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=18)
        self.detail_content.set_margin_top(6)
        self.detail_content.set_margin_bottom(6)
        self.detail_content.set_margin_start(6)
        self.detail_content.set_margin_end(6)

        scroller = Gtk.ScrolledWindow()
        scroller.set_policy(Gtk.PolicyType.NEVER, Gtk.PolicyType.AUTOMATIC)
        scroller.set_child(self.detail_content)

        self.detail_stack.add_named(placeholder, "empty")
        self.detail_stack.add_named(scroller, "detail")
        self.detail_stack.set_visible_child_name("empty")
        return self.detail_stack

    def on_search(self) -> None:
        query = self.query_entry.get_text().strip()
        if not query:
            self.toast("Write something first.")
            return

        self.last_submitted_query = query
        _, self.last_submitted_search_type = SEARCH_TYPES[self.search_type_dropdown.get_selected()]
        self.search_button.set_sensitive(False)
        self.results_count.set_text("Searching…")
        self.results_stack.set_visible_child_name("empty")
        self.clear_detail()
        self.present_loading_dialog("Searching", f"Searching for {query}…")

        self.run_task(
            lambda: self.backend.search(query, self.last_submitted_search_type),
            self.populate_results,
        )

    def on_result_activated(self, _listbox: Gtk.ListBox, row: Gtk.ListBoxRow) -> None:
        band = getattr(row, "band", None)
        if not band or not band.profile_url:
            self.toast("That result has no profile page.")
            return

        self.results_list.set_sensitive(False)
        self.present_loading_dialog("Loading band", f"Fetching details for {band.name or 'selected band'}…")
        self.run_task(
            lambda: self.backend.get_band(band.profile_url),
            self.show_band_detail,
        )

    def on_album_activated(self, _listbox: Gtk.ListBox, row: Gtk.ListBoxRow) -> None:
        album = getattr(row, "album", None)
        if not album or not album.url:
            self.toast("That album has no page.")
            return

        self.present_loading_dialog("Loading album", f"Fetching details for {album.title or 'selected release'}…")
        self.run_task(
            lambda: self.backend.get_album(album.url),
            self.open_album_window,
        )

    def run_task(self, worker, on_success) -> None:
        def runner() -> None:
            try:
                result = worker()
            except Exception as exc:
                GLib.idle_add(self.on_task_error, exc)
                return
            GLib.idle_add(on_success, result)

        threading.Thread(target=runner, daemon=True).start()

    def on_task_error(self, exc: Exception) -> bool:
        self.close_loading_dialog()
        self.search_button.set_sensitive(True)
        self.results_list.set_sensitive(True)
        message = str(exc)
        if isinstance(exc, BackendError) and "moment" in message.lower():
            message = "Metal Archives is still behind Cloudflare. Refresh ~/.config/ferrum/session.json first."
        self.toast(message)
        return False

    def populate_results(self, results: list[BandSummary]) -> bool:
        self.close_loading_dialog()
        self.search_button.set_sensitive(True)
        self.results_list.set_sensitive(True)
        self.remember_search_history_entry(self.last_submitted_query, self.last_submitted_search_type)
        self.results = results
        self.clear_listbox(self.results_list)

        if not results:
            self.results_count.set_text("0 matches")
            self.results_stack.set_visible_child_name("empty")
            self.toast("No results.")
            return False

        for band in results:
            row = self.build_result_row(band)
            row.band = band
            self.results_list.append(row)

        self.results_count.set_text(f"{len(results)} matches")
        self.results_stack.set_visible_child_name("list")
        return False

    def load_search_history(self) -> None:
        def runner() -> None:
            try:
                search_history = self.backend.get_search_history()
            except Exception:
                return
            GLib.idle_add(self.apply_search_history, search_history)

        threading.Thread(target=runner, daemon=True).start()

    def apply_search_history(self, search_history: list[SearchHistoryEntry]) -> bool:
        self.search_history = search_history
        return False

    def present_search_history_dialog(self) -> None:
        if not self.search_history:
            self.toast("No search history yet.")
            return

        dialog = self.create_modal_dialog("Search history", 760, 560)

        content = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=12)
        content.set_vexpand(True)
        content.set_margin_top(18)
        content.set_margin_bottom(18)
        content.set_margin_start(18)
        content.set_margin_end(18)

        title = Gtk.Label(label="Recent searches", xalign=0)
        title.add_css_class("title-3")
        content.append(title)

        history_list = Gtk.ListBox()
        history_list.add_css_class("boxed-list")
        history_list.set_selection_mode(Gtk.SelectionMode.SINGLE)
        history_list.set_vexpand(True)
        history_list.connect("row-activated", self.on_search_history_activated)

        sorted_search_history = sorted(
            self.search_history[:100],
            key=lambda suggestion: suggestion.query.casefold(),
        )
        for suggestion in sorted_search_history:
            row = self.build_search_history_row(suggestion)
            row.search_suggestion = suggestion
            history_list.append(row)

        scroller = Gtk.ScrolledWindow()
        scroller.set_policy(Gtk.PolicyType.NEVER, Gtk.PolicyType.AUTOMATIC)
        scroller.set_hexpand(True)
        scroller.set_vexpand(True)
        scroller.set_child(history_list)
        content.append(scroller)

        dialog.set_child(content)
        self.search_history_dialog = dialog
        self.app.apply_theme()
        dialog.present(self)

    def create_modal_dialog(self, title: str, width: int, height: int) -> Adw.Dialog:
        dialog = Adw.Dialog()
        dialog.set_title(title)
        dialog.set_content_width(width)
        dialog.set_content_height(height)
        dialog.set_follows_content_size(False)
        dialog.set_can_close(True)
        dialog.set_presentation_mode(Adw.DialogPresentationMode.FLOATING)
        backdrop_click = Gtk.GestureClick.new()
        backdrop_click.connect("pressed", self.on_modal_backdrop_pressed, dialog)
        dialog.add_controller(backdrop_click)
        return dialog

    def on_modal_backdrop_pressed(
        self,
        _gesture: Gtk.GestureClick,
        _n_press: int,
        click_x: float,
        click_y: float,
        dialog: Adw.Dialog,
    ) -> None:
        content = dialog.get_child()
        if content is None:
            dialog.close()
            return

        inside_bounds, bounds = content.compute_bounds(dialog)
        if not inside_bounds:
            return

        if self.is_point_inside_rect(click_x, click_y, bounds):
            return

        dialog.close()

    def is_point_inside_rect(self, point_x: float, point_y: float, rect: Graphene.Rect) -> bool:
        rect_x = rect.get_x()
        rect_y = rect.get_y()
        rect_width = rect.get_width()
        rect_height = rect.get_height()
        return (
            rect_x <= point_x <= rect_x + rect_width
            and rect_y <= point_y <= rect_y + rect_height
        )

    def build_search_history_row(self, suggestion: SearchHistoryEntry) -> Gtk.ListBoxRow:
        row = Gtk.ListBoxRow()
        row.set_activatable(True)

        box = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=4)
        box.set_margin_top(10)
        box.set_margin_bottom(10)
        box.set_margin_start(12)
        box.set_margin_end(12)

        query_label = Gtk.Label(label=suggestion.query, xalign=0)
        query_label.add_css_class("title-4")

        type_label = Gtk.Label(label=self.resolve_search_type_label(suggestion.search_type), xalign=0)
        type_label.add_css_class("dim-label")

        box.append(query_label)
        box.append(type_label)
        row.set_child(box)
        return row

    def on_search_history_activated(self, _listbox: Gtk.ListBox, row: Gtk.ListBoxRow) -> None:
        suggestion = getattr(row, "search_suggestion", None)
        if suggestion is None:
            return

        self.query_entry.set_text(suggestion.query)
        self.search_type_dropdown.set_selected(self.resolve_search_type_index(suggestion.search_type))
        if self.search_history_dialog is not None:
            self.search_history_dialog.close()
            self.search_history_dialog = None
        self.query_entry.grab_focus()
        self.query_entry.set_position(-1)
        self.on_search()

    def remember_search_history_entry(self, query: str, search_type: str) -> None:
        normalized_query = query.strip().lower()
        if not normalized_query:
            return

        updated_history = [
            entry
            for entry in self.search_history
            if not (entry.query.lower() == normalized_query and entry.search_type == search_type)
        ]
        updated_history.insert(0, SearchHistoryEntry(query=normalized_query, search_type=search_type))
        self.search_history = updated_history[:100]

    def resolve_search_type_index(self, search_type: str) -> int:
        for index, (_, value) in enumerate(SEARCH_TYPES):
            if value == search_type:
                return index
        return 0

    def resolve_search_type_label(self, search_type: str) -> str:
        for label, value in SEARCH_TYPES:
            if value == search_type:
                return label
        return search_type

    def build_result_row(self, band: BandSummary) -> Gtk.ListBoxRow:
        row = Gtk.ListBoxRow()
        box = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=6)
        box.set_margin_top(12)
        box.set_margin_bottom(12)
        box.set_margin_start(14)
        box.set_margin_end(14)
        box.set_hexpand(True)

        title = Gtk.Label(label=band.name or "Unknown band", xalign=0)
        title.add_css_class("title-4")
        title.set_hexpand(True)
        title.set_ellipsize(Pango.EllipsizeMode.END)
        title.set_lines(1)

        summary = Gtk.Label(
            label="  •  ".join(filter(None, [band.country, band.genre, band.status])),
            xalign=0,
        )
        summary.add_css_class("dim-label")
        summary.set_hexpand(True)
        summary.set_ellipsize(Pango.EllipsizeMode.END)
        summary.set_lines(1)

        box.append(title)
        box.append(summary)
        row.set_child(box)
        return row

    def show_band_detail(self, detail: BandDetail) -> bool:
        self.close_loading_dialog()
        self.search_button.set_sensitive(True)
        self.results_list.set_sensitive(True)
        self.selected_band = detail
        self.clear_box(self.detail_content)

        hero = Gtk.Box(orientation=Gtk.Orientation.HORIZONTAL, spacing=18)
        hero.add_css_class("card")
        hero.add_css_class("hero-card")

        hero_content = Gtk.Box(orientation=Gtk.Orientation.HORIZONTAL, spacing=18)
        hero_content.set_halign(Gtk.Align.START)
        hero_content.set_hexpand(False)

        hero_content.append(
            self.build_remote_artwork(
                detail.image_url,
                296,
                184,
                "band-artwork",
                Gtk.ContentFit.CONTAIN,
            )
        )

        hero_text = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=10)
        hero_text.set_hexpand(True)
        hero_text.set_halign(Gtk.Align.START)

        title = Gtk.Label(label=detail.name or "Unknown band", xalign=0)
        title.add_css_class("title-1")
        title.set_wrap(True)
        title.set_halign(Gtk.Align.START)

        chips = Gtk.FlowBox()
        chips.set_selection_mode(Gtk.SelectionMode.NONE)
        chips.set_max_children_per_line(6)
        chips.set_row_spacing(8)
        chips.set_column_spacing(8)
        chips.set_halign(Gtk.Align.START)
        for value in [detail.country, detail.status, detail.genre]:
            if value:
                chips.insert(self.build_chip(value), -1)

        hero_actions = Gtk.Box(orientation=Gtk.Orientation.HORIZONTAL, spacing=8)
        hero_actions.set_halign(Gtk.Align.START)
        if detail.profile_url:
            profile_button = Gtk.Button(label="Open in browser")
            profile_button.add_css_class("pill")
            profile_button.connect("clicked", lambda *_: webbrowser.open(detail.profile_url))
            hero_actions.append(profile_button)

        hero_text.append(title)
        hero_text.append(chips)
        hero_text.append(hero_actions)
        hero_content.append(hero_text)
        hero.append(hero_content)

        metadata = Gtk.Grid(column_spacing=18, row_spacing=10)
        metadata.add_css_class("card")
        metadata.attach(self.meta_key("Country"), 0, 0, 1, 1)
        metadata.attach(self.meta_value(detail.country), 1, 0, 1, 1)
        metadata.attach(self.meta_key("Location"), 0, 1, 1, 1)
        metadata.attach(self.meta_value(detail.location), 1, 1, 1, 1)
        metadata.attach(self.meta_key("Formed in"), 0, 2, 1, 1)
        metadata.attach(self.meta_value(detail.formed_in), 1, 2, 1, 1)
        metadata.attach(self.meta_key("Years active"), 0, 3, 1, 1)
        metadata.attach(self.meta_value(detail.years_active), 1, 3, 1, 1)
        metadata.attach(self.meta_key("Themes"), 0, 4, 1, 1)
        metadata.attach(self.meta_value(detail.lyrical_themes), 1, 4, 1, 1)
        metadata.attach(self.meta_key("Label"), 0, 5, 1, 1)
        metadata.attach(self.meta_value(detail.label), 1, 5, 1, 1)

        discography_title = Gtk.Label(label="Discography", xalign=0)
        discography_title.add_css_class("title-3")

        discography_header = Gtk.Box(orientation=Gtk.Orientation.HORIZONTAL, spacing=12)
        discography_header.set_halign(Gtk.Align.FILL)
        discography_header.set_hexpand(True)
        discography_title.set_hexpand(True)
        discography_header.append(discography_title)

        self.discography_type_filter_values = self.resolve_discography_type_filters(detail.discography)
        discography_filter_dropdown = Gtk.DropDown.new_from_strings(
            self.resolve_discography_type_filter_labels(self.discography_type_filter_values)
        )
        discography_filter_dropdown.set_selected(0)
        discography_filter_dropdown.connect("notify::selected", self.on_discography_type_filter_selected)
        self.discography_type_filter_dropdown = discography_filter_dropdown
        discography_header.append(discography_filter_dropdown)

        discography_list = Gtk.ListBox()
        discography_list.add_css_class("boxed-list")
        discography_list.set_selection_mode(Gtk.SelectionMode.SINGLE)
        discography_list.connect("row-activated", self.on_album_activated)
        self.discography_list = discography_list
        self.populate_discography_list(detail, None)

        self.detail_content.append(hero)
        self.detail_content.append(metadata)
        self.detail_content.append(discography_header)
        self.detail_content.append(discography_list)
        self.detail_stack.set_visible_child_name("detail")
        return False

    def resolve_discography_type_filters(self, discography: list[AlbumEntry]) -> list[str | None]:
        type_values: list[str] = []
        seen_values: set[str] = set()
        for album in discography:
            resolved_type = _resolve_album_type_name(album.type)
            normalized_type = resolved_type.casefold()
            if normalized_type in seen_values:
                continue
            seen_values.add(normalized_type)
            type_values.append(resolved_type)
        type_values.sort(key=str.casefold)
        return [None, *type_values]

    def resolve_discography_type_filter_labels(self, filters: list[str | None]) -> list[str]:
        return ["All types" if filter_value is None else filter_value for filter_value in filters]

    def on_discography_type_filter_selected(self, dropdown: Gtk.DropDown, _pspec) -> None:
        if self.selected_band is None:
            return
        selected_index = dropdown.get_selected()
        if selected_index >= len(self.discography_type_filter_values):
            return
        selected_type = self.discography_type_filter_values[selected_index]
        self.populate_discography_list(self.selected_band, selected_type)

    def populate_discography_list(self, detail: BandDetail, selected_type: str | None) -> None:
        if self.discography_list is None:
            return

        self.clear_listbox(self.discography_list)
        discography = detail.discography
        if selected_type is not None:
            normalized_selected_type = selected_type.casefold()
            discography = [
                album
                for album in discography
                if _resolve_album_type_name(album.type).casefold() == normalized_selected_type
            ]

        if discography:
            for album in discography:
                row = self.build_album_row(album)
                row.album = album
                self.discography_list.append(row)
            return

        placeholder = Gtk.ListBoxRow()
        placeholder.set_selectable(False)
        empty_box = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=4)
        empty_box.set_margin_top(14)
        empty_box.set_margin_bottom(14)
        empty_box.set_margin_start(14)
        empty_box.set_margin_end(14)
        if detail.discography:
            empty_box.append(_append_classes(Gtk.Label(label="No releases for this filter", xalign=0), "title-4"))
            empty_box.append(_append_classes(Gtk.Label(label="Try another discography type.", xalign=0), "dim-label"))
        else:
            empty_box.append(_append_classes(Gtk.Label(label="No discography loaded", xalign=0), "title-4"))
            empty_box.append(_append_classes(Gtk.Label(label="This band page did not expose a release table.", xalign=0), "dim-label"))
        placeholder.set_child(empty_box)
        self.discography_list.append(placeholder)

    def build_album_row(self, album: AlbumEntry) -> Gtk.ListBoxRow:
        row = Gtk.ListBoxRow()
        if not album.url:
            row.set_activatable(False)
            row.set_selectable(False)
        row.set_child(self.build_album_row_content(album))
        return row

    def build_album_row_content(self, album: AlbumEntry) -> Gtk.Widget:
        box = Gtk.Box(orientation=Gtk.Orientation.HORIZONTAL, spacing=12)
        box.set_margin_top(12)
        box.set_margin_bottom(12)
        box.set_margin_start(14)
        box.set_margin_end(14)

        artwork = self.build_remote_artwork(
            album.image_url,
            64,
            64,
            "discography-artwork",
            Gtk.ContentFit.COVER,
            True,
        )

        text_box = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=4)
        text_box.set_hexpand(True)

        title = Gtk.Label(label=album.title or "Untitled release", xalign=0)
        title.add_css_class("title-4")
        title.set_hexpand(True)
        title.set_ellipsize(Pango.EllipsizeMode.END)
        title.set_lines(1)

        summary_box = Gtk.Box(orientation=Gtk.Orientation.HORIZONTAL, spacing=8)
        summary_box.set_halign(Gtk.Align.START)

        year_label = Gtk.Label(label=album.year or "Unknown year", xalign=0)
        year_label.add_css_class("dim-label")

        separator = Gtk.Label(label="•", xalign=0)
        separator.add_css_class("dim-label")
        type_badge = self.build_album_type_badge(album.type)

        text_box.append(title)
        summary_box.append(year_label)
        summary_box.append(separator)
        summary_box.append(type_badge)
        text_box.append(summary_box)
        box.append(artwork)

        if album.url:
            arrow = Gtk.Image.new_from_icon_name("go-next-symbolic")
            arrow.add_css_class("dim-label")
            box.append(text_box)
            box.append(arrow)
        else:
            text_box.append(_append_classes(Gtk.Label(label="No dedicated album page", xalign=0), "dim-label"))
            box.append(text_box)
        return box

    def open_album_window(self, album: AlbumDetail) -> bool:
        if not self.selected_band:
            self.toast("Band context is missing.")
            return False
        self.close_loading_dialog()
        self.refresh_discography_album(album)
        dialog = AlbumDialog(self.app, album, self.selected_band.name, self.app.settings.music_provider)
        dialog.present(self)
        return False

    def present_loading_dialog(self, title: str, message: str) -> None:
        self.close_loading_dialog()
        self.loading_dialog = LoadingDialog(
            self.app,
            title,
            message,
        )
        self.loading_dialog.present(self)

    def close_loading_dialog(self) -> None:
        if self.loading_dialog is None:
            return
        self.loading_dialog.force_close()
        self.loading_dialog = None

    def refresh_discography_album(self, album: AlbumDetail) -> None:
        if not self.selected_band:
            return
        if not album.url or not album.image_url:
            return

        for discography_album in self.selected_band.discography:
            if discography_album.url == album.url:
                discography_album.image_url = album.image_url
                break

        if self.discography_list is None:
            return

        row = self.discography_list.get_first_child()
        while row is not None:
            current_album = getattr(row, "album", None)
            if current_album is not None and current_album.url == album.url:
                row.set_child(self.build_album_row_content(current_album))
                break
            row = row.get_next_sibling()

    def build_chip(self, text: str) -> Gtk.Widget:
        label = Gtk.Label(label=text, xalign=0)
        label.add_css_class("chip")
        return label

    def build_album_type_badge(self, album_type: str) -> Gtk.Widget:
        resolved_album_type = _resolve_album_type_name(album_type)
        self.app.ensure_album_type_color(resolved_album_type)
        label = Gtk.Label(label=resolved_album_type, xalign=0)
        label.add_css_class("album-type-badge")
        label.add_css_class(_album_type_css_class(resolved_album_type))
        return label

    def build_remote_artwork(
        self,
        image_url: str,
        width: int,
        height: int,
        css_class: str,
        content_fit: Gtk.ContentFit = Gtk.ContentFit.CONTAIN,
        square_crop: bool = False,
    ) -> Gtk.Widget:
        frame = Gtk.Frame()
        frame.add_css_class("artwork-frame")
        frame.add_css_class(css_class)
        frame.set_size_request(width, height)
        frame.set_hexpand(False)
        frame.set_vexpand(False)
        frame.set_halign(Gtk.Align.START)
        frame.set_valign(Gtk.Align.START)
        frame.set_overflow(Gtk.Overflow.HIDDEN)

        stack = Gtk.Stack()
        stack.set_size_request(width, height)
        stack.set_overflow(Gtk.Overflow.HIDDEN)

        placeholder = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=8)
        placeholder.set_size_request(width, height)
        placeholder.set_halign(Gtk.Align.CENTER)
        placeholder.set_valign(Gtk.Align.CENTER)
        placeholder.add_css_class("artwork-placeholder")
        placeholder.set_overflow(Gtk.Overflow.HIDDEN)

        icon = Gtk.Image.new_from_icon_name("image-x-generic-symbolic")
        icon.set_pixel_size(max(32, min(width, height) // 4))
        placeholder.append(icon)

        picture = Gtk.Picture()
        picture.set_can_shrink(True)
        picture.set_content_fit(content_fit)
        picture.set_keep_aspect_ratio(True)
        picture.set_size_request(width, height)
        picture.set_hexpand(False)
        picture.set_vexpand(False)
        picture.set_overflow(Gtk.Overflow.HIDDEN)

        stack.add_named(placeholder, "placeholder")
        stack.add_named(picture, "image")
        stack.set_visible_child_name("placeholder")
        frame.set_child(stack)

        if image_url:
            self.load_remote_artwork(image_url, picture, stack, width, height, square_crop)

        return frame

    def load_remote_artwork(
        self,
        image_url: str,
        picture: Gtk.Picture,
        stack: Gtk.Stack,
        width: int,
        height: int,
        square_crop: bool,
    ) -> None:
        def runner() -> None:
            try:
                response = requests.get(image_url, timeout=30)
                response.raise_for_status()
                texture = self.build_texture(response.content, width, height, square_crop)
            except Exception:
                return

            GLib.idle_add(self.apply_remote_artwork, picture, stack, texture)

        threading.Thread(target=runner, daemon=True).start()

    def build_texture(
        self,
        image_bytes: bytes,
        width: int,
        height: int,
        square_crop: bool,
    ) -> Gdk.Texture:
        if not square_crop:
            return Gdk.Texture.new_from_bytes(GLib.Bytes.new(image_bytes))

        loader = GdkPixbuf.PixbufLoader()
        loader.write(image_bytes)
        loader.close()
        pixbuf = loader.get_pixbuf()
        if pixbuf is None:
            raise ValueError("Could not decode artwork")

        if square_crop:
            pixbuf = self.crop_center_square(pixbuf)

        scaled_pixbuf = pixbuf.scale_simple(width, height, GdkPixbuf.InterpType.BILINEAR)
        if scaled_pixbuf is None:
            raise ValueError("Could not scale artwork")
        return Gdk.Texture.new_for_pixbuf(scaled_pixbuf)

    def crop_center_square(self, pixbuf: GdkPixbuf.Pixbuf) -> GdkPixbuf.Pixbuf:
        crop_size = min(pixbuf.get_width(), pixbuf.get_height())
        offset_x = (pixbuf.get_width() - crop_size) // 2
        offset_y = (pixbuf.get_height() - crop_size) // 2
        return pixbuf.new_subpixbuf(offset_x, offset_y, crop_size, crop_size)

    def apply_remote_artwork(
        self,
        picture: Gtk.Picture,
        stack: Gtk.Stack,
        texture: Gdk.Texture,
    ) -> bool:
        picture.set_paintable(texture)
        stack.set_visible_child_name("image")
        return False

    def meta_key(self, text: str) -> Gtk.Widget:
        label = Gtk.Label(label=text, xalign=0)
        label.add_css_class("meta-key")
        return label

    def meta_value(self, text: str) -> Gtk.Widget:
        label = Gtk.Label(label=text or "—", xalign=0)
        label.set_wrap(True)
        label.add_css_class("meta-value")
        return label

    def clear_detail(self) -> None:
        self.selected_band = None
        self.discography_list = None
        self.discography_type_filter_dropdown = None
        self.discography_type_filter_values = [None]
        self.clear_box(self.detail_content)
        self.detail_stack.set_visible_child_name("empty")

    def clear_listbox(self, listbox: Gtk.ListBox) -> None:
        row = listbox.get_first_child()
        while row is not None:
            next_row = row.get_next_sibling()
            listbox.remove(row)
            row = next_row

    def clear_box(self, box: Gtk.Box) -> None:
        child = box.get_first_child()
        while child is not None:
            next_child = child.get_next_sibling()
            box.remove(child)
            child = next_child

    def toast(self, message: str) -> None:
        self.toast_overlay.add_toast(Adw.Toast.new(GLib.markup_escape_text(message)))


class StartupWindow(Adw.ApplicationWindow):
    def __init__(self, app: Adw.Application) -> None:
        super().__init__(application=app, title="Ferrum", default_width=640, default_height=420)
        self.app = app
        self.backend = FerrumBackend()

        toolbar = Adw.ToolbarView()
        header = Adw.HeaderBar()
        header.pack_end(self.app.build_settings_button(self))
        toolbar.add_top_bar(header)

        self.stack = Gtk.Stack()

        loading_box = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=18)
        loading_box.set_valign(Gtk.Align.CENTER)
        loading_box.set_halign(Gtk.Align.CENTER)
        loading_box.set_margin_top(32)
        loading_box.set_margin_bottom(32)
        loading_box.set_margin_start(32)
        loading_box.set_margin_end(32)

        spinner = Gtk.Spinner()
        spinner.set_spinning(True)
        spinner.set_size_request(48, 48)

        title = Gtk.Label(label="Starting Ferrum backend", xalign=0.5)
        title.add_css_class("title-2")

        subtitle = Gtk.Label(
            label="Launching the Java scraper and waiting for the local API to become available.",
            xalign=0.5,
        )
        subtitle.set_wrap(True)
        subtitle.set_justify(Gtk.Justification.CENTER)
        subtitle.add_css_class("dim-label")

        loading_box.append(spinner)
        loading_box.append(title)
        loading_box.append(subtitle)

        error_page = Adw.StatusPage(
            title="Backend did not start",
            description="Ferrum could not reach the local backend API.",
            icon_name="dialog-error-symbolic",
        )
        retry_button = Gtk.Button(label="Retry")
        retry_button.add_css_class("suggested-action")
        retry_button.connect("clicked", lambda *_: self.start_backend_bootstrap())
        error_page.set_child(retry_button)

        self.stack.add_named(loading_box, "loading")
        self.stack.add_named(error_page, "error")
        self.stack.set_visible_child_name("loading")

        toolbar.set_content(self.stack)
        self.set_content(toolbar)
        self.app.apply_theme()
        self.start_backend_bootstrap()

    def start_backend_bootstrap(self) -> None:
        self.stack.set_visible_child_name("loading")

        def runner() -> None:
            try:
                self.backend.wait_until_ready(timeout_seconds=90)
            except Exception as exc:
                GLib.idle_add(self.on_backend_boot_failed, exc)
                return
            GLib.idle_add(self.on_backend_ready)

        threading.Thread(target=runner, daemon=True).start()

    def on_backend_ready(self) -> bool:
        main_window = FerrumWindow(self.app)
        main_window.present()
        self.close()
        return False

    def on_backend_boot_failed(self, exc: Exception) -> bool:
        page = self.stack.get_child_by_name("error")
        if isinstance(page, Adw.StatusPage):
            page.set_description(str(exc))
        self.stack.set_visible_child_name("error")
        return False


class FerrumApp(Adw.Application):
    def __init__(self) -> None:
        super().__init__(application_id=APP_ID, flags=Gio.ApplicationFlags.NON_UNIQUE)
        self.settings_store = SettingsStore()
        self.settings = self.settings_store.load()
        self.settings_dialog: Adw.Dialog | None = None
        self.dynamic_css_provider = Gtk.CssProvider()
        self.css_loaded = False
        self.connect("activate", self.on_activate)

    def on_activate(self, *_args) -> None:
        self.load_css()
        window = self.props.active_window
        if window is None:
            window = StartupWindow(self)
        self.apply_theme()
        window.present()

    def build_settings_button(self, parent: Gtk.Widget) -> Gtk.Button:
        button = Gtk.Button(label="Settings")
        button.connect("clicked", lambda *_: self.present_settings_dialog(parent))
        return button

    def present_settings_dialog(self, parent: Gtk.Widget) -> None:
        if self.settings_dialog is not None:
            self.settings_dialog.force_close()
            self.settings_dialog = None

        dialog = self.create_modal_dialog("Settings", 720, 560)

        content = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=18)
        content.add_css_class("settings-dialog-content")
        content.set_vexpand(True)
        content.set_margin_top(20)
        content.set_margin_bottom(20)
        content.set_margin_start(20)
        content.set_margin_end(20)

        title = Gtk.Label(label="Preferences", xalign=0)
        title.add_css_class("title-3")

        theme_row = Gtk.Box(orientation=Gtk.Orientation.HORIZONTAL, spacing=16)
        theme_label = Gtk.Label(label="Theme", xalign=0)
        theme_label.set_hexpand(True)
        theme_dropdown = Gtk.DropDown.new_from_strings([label for label, _ in THEME_MODES])
        theme_dropdown.set_selected(self.resolve_theme_index(self.settings.theme_mode))
        theme_dropdown.connect("notify::selected", self.on_theme_selected)
        theme_row.append(theme_label)
        theme_row.append(theme_dropdown)

        provider_row = Gtk.Box(orientation=Gtk.Orientation.HORIZONTAL, spacing=16)
        provider_label = Gtk.Label(label="Music provider", xalign=0)
        provider_label.set_hexpand(True)
        provider_dropdown = Gtk.DropDown.new_from_strings([label for label, _ in PROVIDERS])
        provider_dropdown.set_selected(self.resolve_provider_index(self.settings.music_provider))
        provider_dropdown.connect("notify::selected", self.on_provider_selected)
        provider_row.append(provider_label)
        provider_row.append(provider_dropdown)

        album_type_colors_title = Gtk.Label(label="Album type colors", xalign=0)
        album_type_colors_title.add_css_class("title-4")

        album_type_colors_description = Gtk.Label(
            label="Colors are created automatically the first time a release type appears.",
            xalign=0,
        )
        album_type_colors_description.set_wrap(True)
        album_type_colors_description.add_css_class("dim-label")

        album_type_colors_box = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=12)
        album_type_colors_box.set_vexpand(True)
        if self.settings.album_type_colors:
            for album_type in sorted(self.settings.album_type_colors, key=str.casefold):
                album_type_colors_box.append(self.build_album_type_settings_row(album_type))
        else:
            empty_state = Gtk.Label(
                label="Album types will appear here after you load bands with discography data.",
                xalign=0,
            )
            empty_state.set_wrap(True)
            empty_state.add_css_class("dim-label")
            album_type_colors_box.append(empty_state)

        album_type_colors_scroller = Gtk.ScrolledWindow()
        album_type_colors_scroller.set_policy(Gtk.PolicyType.NEVER, Gtk.PolicyType.AUTOMATIC)
        album_type_colors_scroller.set_min_content_height(280)
        album_type_colors_scroller.set_vexpand(True)
        album_type_colors_scroller.set_child(album_type_colors_box)

        content.append(title)
        content.append(_append_classes(theme_row, "settings-row"))
        content.append(_append_classes(provider_row, "settings-row"))
        content.append(album_type_colors_title)
        content.append(album_type_colors_description)
        content.append(album_type_colors_scroller)

        dialog.set_child(content)
        self.settings_dialog = dialog
        self.apply_theme()
        dialog.present(parent)

    def create_modal_dialog(self, title: str, width: int, height: int) -> Adw.Dialog:
        dialog = Adw.Dialog()
        dialog.set_title(title)
        dialog.set_content_width(width)
        dialog.set_content_height(height)
        dialog.set_follows_content_size(False)
        dialog.set_can_close(True)
        dialog.set_presentation_mode(Adw.DialogPresentationMode.FLOATING)
        backdrop_click = Gtk.GestureClick.new()
        backdrop_click.connect("pressed", self.on_app_modal_backdrop_pressed, dialog)
        dialog.add_controller(backdrop_click)
        return dialog

    def on_app_modal_backdrop_pressed(
        self,
        _gesture: Gtk.GestureClick,
        _n_press: int,
        click_x: float,
        click_y: float,
        dialog: Adw.Dialog,
    ) -> None:
        content = dialog.get_child()
        if content is None:
            dialog.close()
            return

        inside_bounds, bounds = content.compute_bounds(dialog)
        if not inside_bounds:
            return

        if self.is_point_inside_rect(click_x, click_y, bounds):
            return

        dialog.close()

    def is_point_inside_rect(self, point_x: float, point_y: float, rect: Graphene.Rect) -> bool:
        rect_x = rect.get_x()
        rect_y = rect.get_y()
        rect_width = rect.get_width()
        rect_height = rect.get_height()
        return (
            rect_x <= point_x <= rect_x + rect_width
            and rect_y <= point_y <= rect_y + rect_height
        )

    def on_theme_selected(self, dropdown: Gtk.DropDown, _pspec) -> None:
        selected_index = dropdown.get_selected()
        if selected_index >= len(THEME_MODES):
            return

        _, theme_mode = THEME_MODES[selected_index]
        if theme_mode == self.settings.theme_mode:
            return

        self.settings.theme_mode = theme_mode
        self.settings_store.save(self.settings)
        self.apply_theme()

    def on_provider_selected(self, dropdown: Gtk.DropDown, _pspec) -> None:
        selected_index = dropdown.get_selected()
        if selected_index >= len(PROVIDERS):
            return

        _, provider = PROVIDERS[selected_index]
        if provider == self.settings.music_provider:
            return

        self.settings.music_provider = provider
        self.settings_store.save(self.settings)

    def toast(self, message: str) -> None:
        active_window = self.props.active_window
        if active_window is not None and hasattr(active_window, "toast"):
            active_window.toast(message)

    def build_album_type_settings_row(self, album_type: str) -> Gtk.Widget:
        row = Gtk.Box(orientation=Gtk.Orientation.HORIZONTAL, spacing=12)
        row.set_valign(Gtk.Align.CENTER)

        name_label = Gtk.Label(label=album_type, xalign=0)
        name_label.set_hexpand(True)

        preview = self.build_album_type_preview(album_type)

        color_entry = Gtk.Entry()
        color_entry.set_width_chars(8)
        color_entry.set_text(self.settings.album_type_colors.get(album_type, ""))
        color_entry.set_placeholder_text("#RRGGBB")

        save_button = Gtk.Button(label="Save")
        save_button.connect("clicked", self.on_album_type_color_saved, album_type, color_entry)

        randomize_button = Gtk.Button(label="Random")
        randomize_button.connect("clicked", self.on_album_type_color_randomized, album_type, color_entry)

        row.append(name_label)
        row.append(preview)
        row.append(color_entry)
        row.append(save_button)
        row.append(randomize_button)
        return _append_classes(row, "settings-row")

    def build_album_type_preview(self, album_type: str) -> Gtk.Widget:
        preview = Gtk.Label(label=album_type, xalign=0)
        preview.add_css_class("album-type-badge")
        preview.add_css_class(_album_type_css_class(album_type))
        return preview

    def on_album_type_color_saved(
        self,
        _button: Gtk.Button,
        album_type: str,
        color_entry: Gtk.Entry,
    ) -> None:
        normalized_color = normalize_hex_color(color_entry.get_text())
        if normalized_color is None:
            self.toast("Use a valid color like #7C3AED.")
            color_entry.set_text(self.settings.album_type_colors.get(album_type, ""))
            return

        self.settings.album_type_colors[album_type] = normalized_color
        color_entry.set_text(normalized_color)
        self.settings_store.save(self.settings)
        self.reload_dynamic_css()

    def on_album_type_color_randomized(
        self,
        _button: Gtk.Button,
        album_type: str,
        color_entry: Gtk.Entry,
    ) -> None:
        random_color = generate_random_color()
        self.settings.album_type_colors[album_type] = random_color
        color_entry.set_text(random_color)
        self.settings_store.save(self.settings)
        self.reload_dynamic_css()

    def ensure_album_type_color(self, album_type: str) -> str:
        resolved_album_type = _resolve_album_type_name(album_type)
        for current_album_type, color in self.settings.album_type_colors.items():
            if current_album_type.casefold() == resolved_album_type.casefold():
                return color

        generated_color = generate_random_color()
        self.settings.album_type_colors[resolved_album_type] = generated_color
        self.settings_store.save(self.settings)
        self.reload_dynamic_css()
        return generated_color

    def reload_dynamic_css(self) -> None:
        stylesheet = "\n".join(self.build_album_type_color_rules())
        self.dynamic_css_provider.load_from_data(stylesheet.encode("utf-8"))

    def build_album_type_color_rules(self) -> list[str]:
        rules: list[str] = []
        for album_type, color in sorted(self.settings.album_type_colors.items(), key=lambda item: item[0].casefold()):
            css_class = _album_type_css_class(album_type)
            rules.append(
                f".{css_class} {{ "
                f"background: {_rgba(color, 0.16)}; "
                f"color: {_mix_color(color, '#000000', 0.38)}; "
                f"border: 1px solid {_rgba(color, 0.28)}; "
                f"}}"
            )
            rules.append(
                f".ferrum-dark .{css_class} {{ "
                f"background: {_rgba(color, 0.22)}; "
                f"color: {_mix_color(color, '#FFFFFF', 0.58)}; "
                f"border: 1px solid {_rgba(color, 0.34)}; "
                f"}}"
            )
            rules.append(
                f".ferrum-black .{css_class} {{ "
                f"background: {_rgba(color, 0.20)}; "
                f"color: {_mix_color(color, '#FFFFFF', 0.64)}; "
                f"border: 1px solid {_rgba(color, 0.32)}; "
                f"}}"
            )
        return rules

    def apply_theme(self) -> None:
        style_manager = Adw.StyleManager.get_default()
        style_manager.set_color_scheme(self.resolve_color_scheme(self.settings.theme_mode))

        for window in self.get_windows():
            window.remove_css_class("ferrum-light")
            window.remove_css_class("ferrum-dark")
            window.remove_css_class("ferrum-black")
            window.add_css_class(self.resolve_window_theme_class(self.settings.theme_mode))

    def resolve_theme_index(self, theme_mode: str) -> int:
        for index, (_, value) in enumerate(THEME_MODES):
            if value == theme_mode:
                return index
        return 1

    def resolve_provider_index(self, provider: str) -> int:
        for index, (_, value) in enumerate(PROVIDERS):
            if value == provider:
                return index
        return 0

    def resolve_color_scheme(self, theme_mode: str) -> Adw.ColorScheme:
        if theme_mode == "system":
            return Adw.ColorScheme.DEFAULT
        if theme_mode in {"dark", "black"}:
            return Adw.ColorScheme.FORCE_DARK
        return Adw.ColorScheme.FORCE_LIGHT

    def resolve_window_theme_class(self, theme_mode: str) -> str:
        if theme_mode == "black":
            return "ferrum-black"
        if theme_mode == "dark":
            return "ferrum-dark"
        return "ferrum-light"

    def load_css(self) -> None:
        display = Gdk.Display.get_default()
        if display is None:
            return

        if not self.css_loaded:
            provider = Gtk.CssProvider()
            stylesheet = Path(__file__).resolve().parent.parent / "style.css"
            provider.load_from_path(str(stylesheet))
            Gtk.StyleContext.add_provider_for_display(
                display,
                provider,
                Gtk.STYLE_PROVIDER_PRIORITY_APPLICATION,
            )
            Gtk.StyleContext.add_provider_for_display(
                display,
                self.dynamic_css_provider,
                Gtk.STYLE_PROVIDER_PRIORITY_APPLICATION,
            )
            self.css_loaded = True

        self.reload_dynamic_css()


def main() -> None:
    app = FerrumApp()
    app.run(None)


if __name__ == "__main__":
    main()
