(ns clojure.gdx
  (:require [clojure.string :as str])
  (:import (com.badlogic.gdx Gdx
                             Input$Buttons
                             Input$Keys)
           (com.badlogic.gdx.files FileHandle)
           (com.badlogic.gdx.graphics Color
                                      Colors
                                      Pixmap
                                      Pixmap$Format
                                      Texture)
           (com.badlogic.gdx.graphics.g2d Batch
                                          BitmapFont)
           (com.badlogic.gdx.utils Align
                                   ScreenUtils)))

(defmacro post-runnable! [& exprs]
  `(.postRunnable Gdx/app (fn [] ~@exprs)))

(defn internal [path]
  (.internal Gdx/files path))

(defn sound [path]
  (.newSound Gdx/audio (internal path)))

(defn graphics []
  Gdx/graphics)

(defn input []
  Gdx/input)

(defn cursor [path [hotspot-x hotspot-y]]
  (let [pixmap (Pixmap. (internal path))
        cursor (.newCursor Gdx/graphics pixmap hotspot-x hotspot-y)]
    (.dispose pixmap)
    cursor))

(defn set-input-processor! [input-processor]
  (.setInputProcessor Gdx/input input-processor))

; '(clojure.java.interop/k->enum 'class-symbol keyword & :namespaced? false)
; see play-clj
; Make ':bottom-left' -> 'Align/bottomLeft'
; no namespace because its only used @ align or at key-pressed?
; so input.keys is redundant.

(def Align-mapping {:bottom      Align/bottom
                    :bottomleft  Align/bottomLeft
                    :bottomright Align/bottomRight
                    :center      Align/center
                    :left        Align/left
                    :right       Align/right
                    :top         Align/top
                    :topleft     Align/topLeft
                    :topright    Align/topRight})

(defn k->align [k]
  (when-not (contains? Align-mapping k)
    (throw (IllegalArgumentException. (str "Unknown Align: " k ". \nOptions are:\n" (sort (keys Align-mapping))))))
  (k Align-mapping))

(def Input$Buttons-mapping
  {:back    Input$Buttons/BACK
   :forward Input$Buttons/FORWARD
   :left    Input$Buttons/LEFT
   :middle  Input$Buttons/MIDDLE
   :right   Input$Buttons/RIGHT})

(defn k->input-button [k]
  (when-not (contains? Input$Buttons-mapping k)
    (throw (IllegalArgumentException. (str "Unknown Button: " k ". \nOptions are:\n" (sort (keys Input$Buttons-mapping))))))
  (k Input$Buttons-mapping))

(def Input$Keys-mapping
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

(defn k->input-key [k]
  (when-not (contains? Input$Keys-mapping k)
    (throw (IllegalArgumentException. (str "Unknown Key: " k ". \nOptions are:\n" (sort (keys Input$Keys-mapping))))))
  (k Input$Keys-mapping))

(defn get-static-field [mapping k]
  (when-not (contains? mapping k)
    (throw (IllegalArgumentException. (str "Unknown Key: " k ". \nOptions are:\n" (sort (keys mapping))))))
  (k mapping))

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

 (import 'com.badlogic.gdx.utils.Align)
 (generate-mapping "Align" 'int)

 )

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

(defn- k->color [k]
  (when-not (contains? Color-mapping k)
    (throw (IllegalArgumentException. (str "Unknown Color: " k ". \nOptions are:\n" (sort (keys Color-mapping))))))
  (k Color-mapping))

(defn- create-color
  ([r g b]
   (create-color r g b 1))
  ([r g b a]
   (Color. (float r) (float g) (float b) (float a))))

(defn color ^Color [c]
  (cond (= Color (class c)) c
        (keyword? c) (k->color c)
        (vector? c) (apply create-color c)
        :else (throw (ex-info "Cannot understand color" c))))

(defn put-colors! [colors]
  (doseq [[name color-params] colors]
    (Colors/put name (color color-params))))

(defn white-pixel-texture []
  (let [pixmap (doto (Pixmap. 1 1 Pixmap$Format/RGBA8888)
                 (.setColor Color/WHITE)
                 (.drawPixel 0 0))
        texture (Texture. pixmap)]
    (.dispose pixmap)
    texture))

(defn clear-screen! [color*]
  (ScreenUtils/clear (color color*)))

(defn find-assets [{:keys [folder extensions]}]
  (map #(str/replace-first % folder "")
       (loop [[^FileHandle file & remaining] (.list (.internal Gdx/files folder))
              result []]
         (cond (nil? file)
               result

               (.isDirectory file)
               (recur (concat remaining (.list file)) result)

               (extensions (.extension file))
               (recur remaining (conj result (.path file)))

               :else
               (recur remaining result)))))

(defn draw-texture-region! [^Batch batch texture-region [x y] [w h] rotation]
  (.draw batch
         texture-region
         x
         y
         (/ (float w) 2) ; origin-x
         (/ (float h) 2) ; origin-y
         w
         h
         1 ; scale-x
         1 ; scale-y
         rotation))

(defn text-height [^BitmapFont font text]
  (-> text
      (str/split #"\n")
      count
      (* (.getLineHeight font))))

(defn draw-bitmap-font! [{:keys [^BitmapFont font batch text x y target-width align wrap?]}]
  (.draw font
         batch
         text
         (float x)
         (float y)
         (float target-width)
         align
         wrap?))
