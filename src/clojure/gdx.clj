(ns clojure.gdx
  (:require [clojure.assets :as assets]
            [clojure.audio.sound :as sound]
            [clojure.files :as files]
            [clojure.files.file-handle :as file-handle]
            [clojure.graphics :as graphics]
            [clojure.graphics.batch :as batch]
            [clojure.graphics.texture :as texture]
            [clojure.graphics.pixmap :as pixmap]
            [clojure.input :as input]
            [clojure.math.math-utils :as math-utils]
            [clojure.viewport :as viewport])
  (:import (clojure.lang IFn
                         ILookup)
           (com.badlogic.gdx Gdx
                             Input$Buttons
                             Input$Keys)
           (com.badlogic.gdx.assets AssetManager)
           (com.badlogic.gdx.audio Sound)
           (com.badlogic.gdx.files FileHandle)
           (com.badlogic.gdx.graphics Color
                                      Texture
                                      Pixmap
                                      Pixmap$Format
                                      OrthographicCamera)
           (com.badlogic.gdx.graphics.g2d TextureRegion
                                          SpriteBatch)
           (com.badlogic.gdx.math Vector2)
           (com.badlogic.gdx.utils Align
                                   Disposable
                                   ScreenUtils)
           (com.badlogic.gdx.utils.viewport FitViewport)))

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

(def ^:private k->input-button (partial static-field Input$Buttons-mapping "Button"))
(def ^:private k->input-key    (partial static-field Input$Keys-mapping    "Key"))
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

(defn k->align
  "Returns the `com.badlogic.gdx.utils.Align` enum for keyword `k`.

  `k` is either `:center`, `:left` or `:right` and `Align` value is `Align/center`, `Align/left` and `Align/right`."
  [k]
  (case k
    :center Align/center
    :left   Align/left
    :right  Align/right))

(defn- k->class ^Class [asset-type-k]
  (case asset-type-k
    :sound Sound
    :texture Texture))

(defn- reify-asset [asset]
  (if (instance? Sound asset)
    (reify sound/Sound
      (play! [_]
        (Sound/.play asset)))
    (reify texture/Texture
      (region [_]
        (TextureRegion. ^Texture asset))
      (region [_ x y w h]
        (TextureRegion. ^Texture asset
                        (int x)
                        (int y)
                        (int w)
                        (int h))))))

(defn asset-manager [assets]
  (let [this (AssetManager.)]
    (doseq [[file asset-type-k] assets]
      (.load this ^String file (k->class asset-type-k)))
    (.finishLoading this)
    (reify
      Disposable
      (dispose [_]
        (Disposable/.dispose this))

      IFn
      (invoke [_ path]
        (-> (if (.contains this path)
              (.get this ^String path)
              (throw (IllegalArgumentException. (str "Asset cannot be found: " path))))
            reify-asset))

      assets/Assets
      (all-of-type [_ asset-type-k]
        (filter #(= (.getAssetType this %) (k->class asset-type-k))
                (.getAssetNames this))))))

(defn input []
  (let [this Gdx/input]
    (reify input/Input
      (button-just-pressed? [_ button]
        (.isButtonJustPressed this (k->input-button button)))

      (key-pressed? [_ key]
        (.isKeyPressed this (k->input-key key)))

      (key-just-pressed? [_ key]
        (.isKeyJustPressed this (k->input-key key)))

      (set-processor! [_ input-processor]
        (.setInputProcessor this input-processor))

      (mouse-position [_]
        [(.getX this)
         (.getY this)]))))

(defn- reify-file-handle [^FileHandle fh]
  (reify
    ILookup
    (valAt [_ k]
      (case k
        :java-object fh)) ; Pixmap. & FreeTypeFontGenerator.
    file-handle/FileHandle
    (list [_]
      (map reify-file-handle (.list fh)))
    (directory? [_]
      (.isDirectory fh))
    (extension [_]
      (.extension fh))
    (path [_]
      (.path fh))))

(defn files []
  (let [this Gdx/files]
    (reify files/Files
      (internal [_ path]
        (reify-file-handle (.internal this path))))))

(defn graphics []
  (let [this Gdx/graphics]
    (reify graphics/Graphics
      (delta-time [_]
        (.getDeltaTime this))

      (frames-per-second [_]
        (.getFramesPerSecond this))

      (new-cursor [_ pixmap hotspot-x hotspot-y]
        (.newCursor this pixmap hotspot-x hotspot-y))

      (set-cursor! [_ cursor]
        (.setCursor this cursor)))))

(defn sprite-batch []
  (let [this (SpriteBatch.)]
    (reify
       ILookup
       (valAt [_ k]
         (case k
           :sprite-batch/java-object this))

      Disposable
      (dispose [_]
        (.dispose this))

      batch/Batch
      (set-color! [_ color]
        (.setColor this ^Color color))

      (draw! [_ texture-region {:keys [x
                                       y
                                       origin-x
                                       origin-y
                                       width
                                       height
                                       scale-x
                                       scale-y
                                       rotation]}]
        (.draw this
               texture-region
               x
               y
               origin-x
               origin-y
               width
               height
               scale-x
               scale-y
               rotation))

      (begin! [_]
        (.begin this))

      (end! [_]
        (.end this))

      (set-projection-matrix! [_ matrix]
        (.setProjectionMatrix this matrix)))))

(defn pixmap
  ([file-handle]
   (Pixmap. ^FileHandle (:java-object file-handle)) ; reify ? used @ create-cursor ...
   )
  ([width height format]
   (let [this (Pixmap. (int width)
                       (int height)
                       ^Pixmap$Format
                       (case format
                         :pixmap.format/RGBA8888 Pixmap$Format/RGBA8888))]
     (reify
       Disposable
       (dispose [_]
         (.dispose this))
       ILookup
       (valAt [_ k]
         (case k
           :pixmap/java-object this))
       pixmap/Pixmap
       (set-color! [_ color]
         (.setColor this ^Color color))
       (draw-pixel! [_ x y]
         (.drawPixel this x y))))))

(defn texture [pixmap]
  (Texture. ^Pixmap (:pixmap/java-object pixmap)))

(defn- fit-viewport [width height camera {:keys [center-camera?]}]
  (let [this (FitViewport. width height camera)]
    (reify
      viewport/Viewport
      (resize! [_ width height]
        (.update this width height center-camera?))

      ; touch coordinates are y-down, while screen coordinates are y-up
      ; so the clamping of y is reverse, but as black bars are equal it does not matter
      ; TODO clamping only works for gui-viewport ?
      ; TODO ? "Can be negative coordinates, undefined cells."
      (unproject [_ [x y]]
        (let [clamped-x (math-utils/clamp x
                                          (.getLeftGutterWidth this)
                                          (.getRightGutterX    this))
              clamped-y (math-utils/clamp y
                                          (.getTopGutterHeight this)
                                          (.getTopGutterY      this))]
          (let [v2 (.unproject this (Vector2. clamped-x clamped-y))]
            [(.x v2) (.y v2)])))

      ILookup
      (valAt [_ key]
        (case key
          :java-object this
          :width  (.getWorldWidth  this)
          :height (.getWorldHeight this)
          :camera (.getCamera      this))))))

(defn ui-viewport [{:keys [width height]}]
  (fit-viewport width
                height
                (OrthographicCamera.)
                {:center-camera? true}))

(defn world-viewport [world-unit-scale {:keys [width height]}]
  (let [camera (OrthographicCamera.)
        world-width  (* width world-unit-scale)
        world-height (* height world-unit-scale)
        y-down? false]
    (.setToOrtho camera y-down? world-width world-height)
    (fit-viewport world-width
                  world-height
                  camera
                  {:center-camera? false})))

(defmacro post-runnable! [& exprs]
  `(.postRunnable Gdx/app (fn [] ~@exprs)))

(defn clear-screen! []
  (ScreenUtils/clear Color/BLACK))
