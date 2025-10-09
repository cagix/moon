(ns com.badlogic.gdx
  (:require [clojure.audio]
            [clojure.color :as color]
            [clojure.input]
            [clojure.sound]
            [clojure.gdx :as gdx]
            [clojure.gdx.bitmap-font :as bitmap-font]
            [clojure.gdx.shape-drawer :as shape-drawer]
            [clojure.gdx.math.vector2 :as vector2]
            [clojure.gdx.math.vector3 :as vector3]
            [clojure.gdx.orthographic-camera :as orthographic-camera]
            [clojure.gdx.viewport :as viewport]
            [clojure.string :as str]
            [clojure.math :as math])
  (:import (cdq.ui Stage)
           (com.badlogic.gdx Audio
                             Files
                             Gdx
                             Graphics
                             Input)
           (com.badlogic.gdx.audio Sound)
           (com.badlogic.gdx.files FileHandle)
           (com.badlogic.gdx.graphics GL20
                                      Pixmap
                                      Texture
                                      Texture$TextureFilter
                                      OrthographicCamera)
           (com.badlogic.gdx.graphics.g2d BitmapFont
                                          SpriteBatch)
           (com.badlogic.gdx.graphics.g2d.freetype FreeTypeFontGenerator
                                                   FreeTypeFontGenerator$FreeTypeFontParameter)
           (com.badlogic.gdx.utils Align)
           (com.badlogic.gdx.utils.viewport FitViewport
                                            Viewport)
           (space.earlygrey.shapedrawer ShapeDrawer)))

(defn- recursively-search
  [^FileHandle folder extensions]
  (loop [[^FileHandle file & remaining] (.list folder)
         result []]
    (cond (nil? file)
          result

          (.isDirectory file)
          (recur (concat remaining (.list file)) result)

          (extensions (.extension file))
          (recur remaining (conj result (.path file)))

          :else
          (recur remaining result))))

(defrecord Context [^Audio audio
                    ^Files files
                    ^Graphics graphics
                    ^Input input]
  clojure.audio/Audio
  (sound [_ path]
    (.newSound audio (.internal files path)))

  gdx/Graphics
  (sprite-batch [_]
    (SpriteBatch.))

  (cursor [_ path [hotspot-x hotspot-y]]
    (let [pixmap (Pixmap. (.internal files path))
          cursor (.newCursor graphics pixmap hotspot-x hotspot-y)]
      (.dispose pixmap)
      cursor))

  (truetype-font [_ path {:keys [size
                                 quality-scaling
                                 enable-markup?
                                 use-integer-positions?]}]
    (let [generator (FreeTypeFontGenerator. (.internal files path))
          font (.generateFont generator
                              (let [params (FreeTypeFontGenerator$FreeTypeFontParameter.)]
                                (set! (.size params) (* size quality-scaling))
                                (set! (.minFilter params) Texture$TextureFilter/Linear)
                                (set! (.magFilter params) Texture$TextureFilter/Linear)
                                params))]
      (.setScale (.getData font) (/ quality-scaling))
      (set! (.markupEnabled (.getData font)) enable-markup?)
      (.setUseIntegerPositions font use-integer-positions?)
      font))

  (shape-drawer [_ batch texture-region]
    (ShapeDrawer. batch texture-region))

  (texture [_ path]
    (Texture. (.internal files path)))

  (viewport [_ world-width world-height camera]
    (FitViewport. world-width world-height camera))

  (delta-time [_]
    (.getDeltaTime graphics))

  (frames-per-second [_]
    (.getFramesPerSecond graphics))

  (set-cursor! [_ cursor]
    (.setCursor graphics cursor))

  (clear! [_ [r g b a]]
    (let [clear-depth? false
          apply-antialiasing? false
          gl20 (.getGL20 graphics)]
      (GL20/.glClearColor gl20 r g b a)
      (let [mask (cond-> GL20/GL_COLOR_BUFFER_BIT
                   clear-depth? (bit-or GL20/GL_DEPTH_BUFFER_BIT)
                   (and apply-antialiasing? (.coverageSampling (.getBufferFormat graphics)))
                   (bit-or GL20/GL_COVERAGE_BUFFER_BIT_NV))]
        (GL20/.glClear gl20 mask))))

  (orthographic-camera [_]
    (OrthographicCamera.))

  (orthographic-camera[_ {:keys [y-down? world-width world-height]}]
    (doto (OrthographicCamera.)
      (.setToOrtho y-down? world-width world-height)))

  gdx/Files
  (search-files [_ {:keys [folder extensions]}]
    (map (fn [path]
         (str/replace-first path folder ""))
       (recursively-search (.internal files folder) extensions)))

  clojure.input/Input
  (set-processor! [_ input-processor]
    (.setInputProcessor input input-processor))

  (key-pressed? [_ key]
    (.isKeyPressed input key))

  (key-just-pressed? [_ key]
    (.isKeyJustPressed input key))

  (button-just-pressed? [_ button]
    (.isButtonJustPressed input button))

  (mouse-position [_]
    [(.getX input)
     (.getY input)])

  gdx/Scene2d
  (stage [_ viewport batch]
    (Stage. viewport batch))
  )

(defn context []
  (map->Context
   {:audio    Gdx/audio
    :files    Gdx/files
    :graphics Gdx/graphics
    :input    Gdx/input}))

(extend-type Sound
  clojure.sound/Sound
  (play! [this]
    (.play this))
  (dispose! [this]
    (.dispose this)))

(defn- text-height [^BitmapFont font text]
  (-> text
      (str/split #"\n")
      count
      (* (.getLineHeight font))))

(extend-type BitmapFont
  bitmap-font/BitmapFont
  (draw! [font batch {:keys [scale text x y up? h-align target-width wrap?]}]
    (let [old-scale (.scaleX (.getData font))]
      (.setScale (.getData font) (* old-scale scale))
      (.draw font
             batch
             text
             (float x)
             (float (+ y (if up? (text-height font text) 0)))
             (float target-width)
             (or h-align Align/center)
             wrap?)
      (.setScale (.getData font) old-scale))))

(extend-type Viewport
  viewport/Viewport
  (camera [this]
    (.getCamera this))

  (update! [viewport width height {:keys [center?]}]
    (.update viewport width height (boolean center?)))

  (world-width [this]
    (.getWorldWidth this))

  (world-height [this]
    (.getWorldHeight this))

  (unproject [viewport [x y]]
    (-> viewport
        (.unproject (vector2/->java x y))
        vector2/->clj)))

(extend-type ShapeDrawer
  shape-drawer/ShapeDrawer
  (arc! [this [center-x center-y] radius start-angle degree color]
    (.setColor this (color/float-bits color))
    (.arc this
          center-x
          center-y
          radius
          (math/to-radians start-angle)
          (math/to-radians degree)) )

  (circle! [this [x y] radius color]
    (.setColor this (color/float-bits color))
    (.circle this x y radius))

  (ellipse! [this [x y] radius-x radius-y color]
    (.setColor this (color/float-bits color))
    (.ellipse this x y radius-x radius-y))

  (filled-ellipse! [this [x y] radius-x radius-y color]
    (.setColor this (color/float-bits color))
    (.filledEllipse this x y radius-x radius-y) )

  (filled-circle! [this [x y] radius color]
    (.setColor this (color/float-bits color))
    (.filledCircle this (float x) (float y) (float radius)))

  (filled-rectangle! [this x y w h color]
    (.setColor this (color/float-bits color))
    (.filledRectangle this (float x) (float y) (float w) (float h)))

  (line! [this [sx sy] [ex ey] color]
    (.setColor this (color/float-bits color))
    (.line this (float sx) (float sy) (float ex) (float ey)))

  (rectangle! [this x y w h color]
    (.setColor this (color/float-bits color))
    (.rectangle this x y w h) )

  (sector! [this [center-x center-y] radius start-angle degree color]
    (.setColor this (color/float-bits color))
    (.sector this
             center-x
             center-y
             radius
             (math/to-radians start-angle)
             (math/to-radians degree))))

(extend-type OrthographicCamera
  orthographic-camera/OrthographicCamera
  (viewport-height [camera]
    (.viewportHeight camera))

  (viewport-width [camera]
    (.viewportWidth camera))

  (position [camera]
    (vector3/clojurize (.position camera)))

  (zoom [camera]
    (.zoom camera))

  (combined [camera]
    (.combined camera))

  (set-position! [this [x y]]
    (set! (.x (.position this)) (float x))
    (set! (.y (.position this)) (float y))
    (.update this))

  (set-zoom! [this amount]
    (set! (.zoom this) amount)
    (.update this))

  (frustum-bounds [this]
    (mapv vector3/clojurize (.planePoints (.frustum this)))))
