(ns clojure.gdx
  (:require [gdl.tiled :as tiled]
            [gdl.utils.disposable :as disposable])
  (:import (clojure.lang ILookup)
           (com.badlogic.gdx Gdx
                             Input$Buttons
                             Input$Keys)
           (com.badlogic.gdx.graphics Color
                                      Colors)
           (com.badlogic.gdx.graphics.g2d TextureRegion)
           (com.badlogic.gdx.maps MapProperties)
           (com.badlogic.gdx.maps.tiled TiledMap
                                        TiledMapTileLayer
                                        TiledMapTileLayer$Cell
                                        TmxMapLoader)
           (com.badlogic.gdx.maps.tiled.tiles StaticTiledMapTile)
           (com.badlogic.gdx.utils Align
                                   ScreenUtils)))

(comment

 (require '[clojure.string :as str]
          '[clojure.reflect :refer [type-reflect]])

 (defn- relevant-fields [class-str field-type]
   (->> class-str
        symbol
        eval
        type-reflect
        :members
        (filter #(= field-type (:type %)))))

 (defn- ->clojure-symbol [field]
   (-> field :name name str/lower-case (str/replace #"_" "-") symbol))

 (defn create-mapping [class-str field-type]
   (sort-by first
            (for [field (relevant-fields class-str field-type)]
              [(keyword (->clojure-symbol field))
               (symbol class-str (str (:name field)))])))

 (defn generate-mapping [class-str field-type]
   (spit "temp.clj"
         (with-out-str
          (println "{")
          (doseq [[kw static-field] (create-mapping class-str field-type)]
            (println kw static-field))
          (println "}"))))

 (generate-mapping "Input$Buttons" 'int)
 (generate-mapping "Input$Keys"    'int) ; without Input$Keys/MAX_KEYCODE
 (generate-mapping "Color" 'com.badlogic.gdx.graphics.Color)

 )

(def ^:private Input$Buttons-mapping
  {:back    Input$Buttons/BACK
   :forward Input$Buttons/FORWARD
   :left    Input$Buttons/LEFT
   :middle  Input$Buttons/MIDDLE
   :right   Input$Buttons/RIGHT})

(def ^:private Input$Keys-mapping
  {:a                   Input$Keys/A
   :alt-left            Input$Keys/ALT_LEFT
   :alt-right           Input$Keys/ALT_RIGHT
   :any-key             Input$Keys/ANY_KEY
   :apostrophe          Input$Keys/APOSTROPHE
   :at                  Input$Keys/AT
   :b                   Input$Keys/B
   :back                Input$Keys/BACK
   :backslash           Input$Keys/BACKSLASH
   :backspace           Input$Keys/BACKSPACE
   :button-a            Input$Keys/BUTTON_A
   :button-b            Input$Keys/BUTTON_B
   :button-c            Input$Keys/BUTTON_C
   :button-circle       Input$Keys/BUTTON_CIRCLE
   :button-l1           Input$Keys/BUTTON_L1
   :button-l2           Input$Keys/BUTTON_L2
   :button-mode         Input$Keys/BUTTON_MODE
   :button-r1           Input$Keys/BUTTON_R1
   :button-r2           Input$Keys/BUTTON_R2
   :button-select       Input$Keys/BUTTON_SELECT
   :button-start        Input$Keys/BUTTON_START
   :button-thumbl       Input$Keys/BUTTON_THUMBL
   :button-thumbr       Input$Keys/BUTTON_THUMBR
   :button-x            Input$Keys/BUTTON_X
   :button-y            Input$Keys/BUTTON_Y
   :button-z            Input$Keys/BUTTON_Z
   :c                   Input$Keys/C
   :call                Input$Keys/CALL
   :camera              Input$Keys/CAMERA
   :caps-lock           Input$Keys/CAPS_LOCK
   :center              Input$Keys/CENTER
   :clear               Input$Keys/CLEAR
   :colon               Input$Keys/COLON
   :comma               Input$Keys/COMMA
   :control-left        Input$Keys/CONTROL_LEFT
   :control-right       Input$Keys/CONTROL_RIGHT
   :d                   Input$Keys/D
   :del                 Input$Keys/DEL
   :down                Input$Keys/DOWN
   :dpad-center         Input$Keys/DPAD_CENTER
   :dpad-down           Input$Keys/DPAD_DOWN
   :dpad-left           Input$Keys/DPAD_LEFT
   :dpad-right          Input$Keys/DPAD_RIGHT
   :dpad-up             Input$Keys/DPAD_UP
   :e                   Input$Keys/E
   :end                 Input$Keys/END
   :endcall             Input$Keys/ENDCALL
   :enter               Input$Keys/ENTER
   :envelope            Input$Keys/ENVELOPE
   :equals              Input$Keys/EQUALS
   :escape              Input$Keys/ESCAPE
   :explorer            Input$Keys/EXPLORER
   :f                   Input$Keys/F
   :f1                  Input$Keys/F1
   :f10                 Input$Keys/F10
   :f11                 Input$Keys/F11
   :f12                 Input$Keys/F12
   :f13                 Input$Keys/F13
   :f14                 Input$Keys/F14
   :f15                 Input$Keys/F15
   :f16                 Input$Keys/F16
   :f17                 Input$Keys/F17
   :f18                 Input$Keys/F18
   :f19                 Input$Keys/F19
   :f2                  Input$Keys/F2
   :f20                 Input$Keys/F20
   :f21                 Input$Keys/F21
   :f22                 Input$Keys/F22
   :f23                 Input$Keys/F23
   :f24                 Input$Keys/F24
   :f3                  Input$Keys/F3
   :f4                  Input$Keys/F4
   :f5                  Input$Keys/F5
   :f6                  Input$Keys/F6
   :f7                  Input$Keys/F7
   :f8                  Input$Keys/F8
   :f9                  Input$Keys/F9
   :focus               Input$Keys/FOCUS
   :forward-del         Input$Keys/FORWARD_DEL
   :g                   Input$Keys/G
   :grave               Input$Keys/GRAVE
   :h                   Input$Keys/H
   :headsethook         Input$Keys/HEADSETHOOK
   :home                Input$Keys/HOME
   :i                   Input$Keys/I
   :insert              Input$Keys/INSERT
   :j                   Input$Keys/J
   :k                   Input$Keys/K
   :l                   Input$Keys/L
   :left                Input$Keys/LEFT
   :left-bracket        Input$Keys/LEFT_BRACKET
   :m                   Input$Keys/M
   :media-fast-forward  Input$Keys/MEDIA_FAST_FORWARD
   :media-next          Input$Keys/MEDIA_NEXT
   :media-play-pause    Input$Keys/MEDIA_PLAY_PAUSE
   :media-previous      Input$Keys/MEDIA_PREVIOUS
   :media-rewind        Input$Keys/MEDIA_REWIND
   :media-stop          Input$Keys/MEDIA_STOP
   :menu                Input$Keys/MENU
   :meta-alt-left-on    Input$Keys/META_ALT_LEFT_ON
   :meta-alt-on         Input$Keys/META_ALT_ON
   :meta-alt-right-on   Input$Keys/META_ALT_RIGHT_ON
   :meta-shift-left-on  Input$Keys/META_SHIFT_LEFT_ON
   :meta-shift-on       Input$Keys/META_SHIFT_ON
   :meta-shift-right-on Input$Keys/META_SHIFT_RIGHT_ON
   :meta-sym-on         Input$Keys/META_SYM_ON
   :minus               Input$Keys/MINUS
   :mute                Input$Keys/MUTE
   :n                   Input$Keys/N
   :notification        Input$Keys/NOTIFICATION
   :num                 Input$Keys/NUM
   :num-0               Input$Keys/NUM_0
   :num-1               Input$Keys/NUM_1
   :num-2               Input$Keys/NUM_2
   :num-3               Input$Keys/NUM_3
   :num-4               Input$Keys/NUM_4
   :num-5               Input$Keys/NUM_5
   :num-6               Input$Keys/NUM_6
   :num-7               Input$Keys/NUM_7
   :num-8               Input$Keys/NUM_8
   :num-9               Input$Keys/NUM_9
   :num-lock            Input$Keys/NUM_LOCK
   :numpad-0            Input$Keys/NUMPAD_0
   :numpad-1            Input$Keys/NUMPAD_1
   :numpad-2            Input$Keys/NUMPAD_2
   :numpad-3            Input$Keys/NUMPAD_3
   :numpad-4            Input$Keys/NUMPAD_4
   :numpad-5            Input$Keys/NUMPAD_5
   :numpad-6            Input$Keys/NUMPAD_6
   :numpad-7            Input$Keys/NUMPAD_7
   :numpad-8            Input$Keys/NUMPAD_8
   :numpad-9            Input$Keys/NUMPAD_9
   :numpad-add          Input$Keys/NUMPAD_ADD
   :numpad-comma        Input$Keys/NUMPAD_COMMA
   :numpad-divide       Input$Keys/NUMPAD_DIVIDE
   :numpad-dot          Input$Keys/NUMPAD_DOT
   :numpad-enter        Input$Keys/NUMPAD_ENTER
   :numpad-equals       Input$Keys/NUMPAD_EQUALS
   :numpad-left-paren   Input$Keys/NUMPAD_LEFT_PAREN
   :numpad-multiply     Input$Keys/NUMPAD_MULTIPLY
   :numpad-right-paren  Input$Keys/NUMPAD_RIGHT_PAREN
   :numpad-subtract     Input$Keys/NUMPAD_SUBTRACT
   :o                   Input$Keys/O
   :p                   Input$Keys/P
   :page-down           Input$Keys/PAGE_DOWN
   :page-up             Input$Keys/PAGE_UP
   :pause               Input$Keys/PAUSE
   :period              Input$Keys/PERIOD
   :pictsymbols         Input$Keys/PICTSYMBOLS
   :plus                Input$Keys/PLUS
   :pound               Input$Keys/POUND
   :power               Input$Keys/POWER
   :print-screen        Input$Keys/PRINT_SCREEN
   :q                   Input$Keys/Q
   :r                   Input$Keys/R
   :right               Input$Keys/RIGHT
   :right-bracket       Input$Keys/RIGHT_BRACKET
   :s                   Input$Keys/S
   :scroll-lock         Input$Keys/SCROLL_LOCK
   :search              Input$Keys/SEARCH
   :semicolon           Input$Keys/SEMICOLON
   :shift-left          Input$Keys/SHIFT_LEFT
   :shift-right         Input$Keys/SHIFT_RIGHT
   :slash               Input$Keys/SLASH
   :soft-left           Input$Keys/SOFT_LEFT
   :soft-right          Input$Keys/SOFT_RIGHT
   :space               Input$Keys/SPACE
   :star                Input$Keys/STAR
   :switch-charset      Input$Keys/SWITCH_CHARSET
   :sym                 Input$Keys/SYM
   :t                   Input$Keys/T
   :tab                 Input$Keys/TAB
   :u                   Input$Keys/U
   :unknown             Input$Keys/UNKNOWN
   :up                  Input$Keys/UP
   :v                   Input$Keys/V
   :volume-down         Input$Keys/VOLUME_DOWN
   :volume-up           Input$Keys/VOLUME_UP
   :w                   Input$Keys/W
   :x                   Input$Keys/X
   :y                   Input$Keys/Y
   :z                   Input$Keys/Z})

(def ^:private Color-mapping
  {:black       Color/BLACK
   :blue        Color/BLUE
   :brown       Color/BROWN
   :chartreuse  Color/CHARTREUSE
   :clear       Color/CLEAR
   :clear-white Color/CLEAR_WHITE
   :coral       Color/CORAL
   :cyan        Color/CYAN
   :dark-gray   Color/DARK_GRAY
   :firebrick   Color/FIREBRICK
   :forest      Color/FOREST
   :gold        Color/GOLD
   :goldenrod   Color/GOLDENROD
   :gray        Color/GRAY
   :green       Color/GREEN
   :light-gray  Color/LIGHT_GRAY
   :lime        Color/LIME
   :magenta     Color/MAGENTA
   :maroon      Color/MAROON
   :navy        Color/NAVY
   :olive       Color/OLIVE
   :orange      Color/ORANGE
   :pink        Color/PINK
   :purple      Color/PURPLE
   :red         Color/RED
   :royal       Color/ROYAL
   :salmon      Color/SALMON
   :scarlet     Color/SCARLET
   :sky         Color/SKY
   :slate       Color/SLATE
   :tan         Color/TAN
   :teal        Color/TEAL
   :violet      Color/VIOLET
   :white       Color/WHITE
   :yellow      Color/YELLOW})

(defn- static-field [mapping exception-name k]
  (when-not (contains? mapping k)
    (throw (IllegalArgumentException. (str "Unknown " exception-name ": " k ". \nOptions are:\n" (sort (keys mapping))))))
  (k mapping))

(def k->input-button (partial static-field Input$Buttons-mapping "Button"))
(def k->input-key    (partial static-field Input$Keys-mapping    "Key"))
(def ^:private k->color        (partial static-field Color-mapping         "Color"))

(defn- create-color
  ([r g b]
   (create-color r g b 1))
  ([r g b a]
   (Color. (float r) (float g) (float b) (float a))))

(defn ->color ^Color [c]
  (cond (= Color (class c)) c
        (keyword? c) (k->color c)
        (vector? c) (apply create-color c)
        :else (throw (ex-info "Cannot understand color" c))))

(defn add-markdown-color! [name color]
  (Colors/put name (->color color)))

(defn k->align
  "Returns the `com.badlogic.gdx.utils.Align` enum for keyword `k`.

  `k` is either `:center`, `:left` or `:right` and `Align` value is `Align/center`, `Align/left` and `Align/right`."
  [k]
  (case k
    :center Align/center
    :left   Align/left
    :right  Align/right))

(defmacro post-runnable! [& exprs]
  `(.postRunnable Gdx/app (fn [] ~@exprs)))

(defn clear-screen! []
  (ScreenUtils/clear Color/BLACK))

(defprotocol GetMapProperties
  (get-map-properties ^MapProperties [_]))

(extend-protocol GetMapProperties
  TiledMap          (get-map-properties [this] (.getProperties this))
  TiledMapTileLayer (get-map-properties [this] (.getProperties this)))

(defn- build-map-properties [obj]
  (let [ps (get-map-properties obj)]
    (zipmap (.getKeys ps) (.getValues ps))))

(defn- add-map-properties! [has-map-properties properties]
  (doseq [[k v] properties]
    (assert (string? k))
    (.put (get-map-properties has-map-properties) k v)))

(defn- reify-tiled-layer [^TiledMapTileLayer this]
  (reify
    ILookup
    (valAt [_ key]
      (.get (.getProperties this) key))

    tiled/HasMapProperties
    (map-properties [_]
      (build-map-properties this))

    tiled/TiledMapTileLayer
    (set-visible! [_ boolean]
      (.setVisible this boolean))

    (visible? [_]
      (.isVisible this))

    (layer-name [_]
      (.getName this))

    (tile-at [_ [x y]]
      (when-let [cell (.getCell this x y)]
        (.getTile cell)))

    (property-value [_ [x y] property-key]
      (if-let [cell (.getCell this x y)]
        (if-let [value (.get (.getProperties (.getTile cell)) property-key)]
          value
          :undefined)
        :no-cell))))

(defn- add-tiled-map-tile-layer!
  "Returns nil."
  [^TiledMap tiled-map
   {:keys [name
           visible?
           properties
           tiles]}]
  {:pre [(string? name)
         (boolean? visible?)]}
  (let [tm-props (.getProperties tiled-map)
        layer (TiledMapTileLayer. (.get tm-props "width")
                                  (.get tm-props "height")
                                  (.get tm-props "tilewidth")
                                  (.get tm-props "tileheight"))]
    (.setName layer name)
    (.setVisible layer visible?)
    (add-map-properties! layer properties)
    (doseq [[[x y] tiled-map-tile] tiles
            :when tiled-map-tile]
      (.setCell layer x y (doto (TiledMapTileLayer$Cell.)
                            (.setTile tiled-map-tile))))
    (.add (.getLayers tiled-map) layer)
    nil))

(defn- reify-tiled-map [^TiledMap this]
  (reify
    disposable/Disposable
    (dispose! [_]
      (.dispose this))

    ILookup
    (valAt [_ key]
      (case key
        :tiled-map/java-object this
        :tiled-map/width  (.get (.getProperties this) "width")
        :tiled-map/height (.get (.getProperties this) "height")))

    tiled/HasMapProperties
    (map-properties [_]
      (build-map-properties this))

    tiled/TiledMap
    (layers [_]
      (map reify-tiled-layer (.getLayers this)))

    (layer-index [_ layer]
      (let [idx (.getIndex (.getLayers this) ^String (tiled/layer-name layer))]
        (when-not (= idx -1)
          idx)))

    (get-layer [_ layer-name]
      (reify-tiled-layer (.get (.getLayers this) ^String layer-name)))

    (add-layer! [_ layer-declaration]
      (add-tiled-map-tile-layer! this layer-declaration))))

(defn tmx-tiled-map
  "Has to be disposed because it loads textures.
  Loads through internal file handle."
  [file-name]
  (reify-tiled-map
   (.load (TmxMapLoader.) file-name)))

(defn create-tiled-map [{:keys [properties
                                layers]}]
  (let [tiled-map (TiledMap.)]
    (add-map-properties! tiled-map properties)
    (doseq [layer layers]
      (add-tiled-map-tile-layer! tiled-map layer))
    (reify-tiled-map tiled-map)))

; why this ? can't I just reuse them?
; probably something with dispose old maps and lose texture resources associated with it
(def copy-tile
  "Memoized function.
  Tiles are usually shared by multiple cells.
  https://libgdx.com/wiki/graphics/2d/tile-maps#cells
  No copied-tile for AnimatedTiledMapTile yet (there was no copy constructor/method)"
  (memoize
   (fn [^StaticTiledMapTile tile]
     (assert tile)
     (StaticTiledMapTile. tile))))

; probably memoize over texture-region data not the java obj itself
; -> memory leak when creating too many maps
(defn static-tiled-map-tile [texture-region property-name property-value]
  {:pre [(:texture-region/java-object texture-region)
         (string? property-name)]}
  (let [tile (StaticTiledMapTile. ^TextureRegion (:texture-region/java-object texture-region))]
    (.put (.getProperties tile) property-name property-value)
    tile))
