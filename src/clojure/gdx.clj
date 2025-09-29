(ns clojure.gdx
  (:require clojure.audio
            clojure.audio.sound
            clojure.disposable
            clojure.files
            clojure.files.file-handle
            clojure.graphics
            clojure.graphics.batch
            clojure.graphics.bitmap-font
            clojure.graphics.orthographic-camera
            clojure.graphics.texture-region
            [clojure.string :as str]
            [com.badlogic.gdx.graphics.color :as color]
            [com.badlogic.gdx.graphics.texture.filter :as texture-filter]
            [com.badlogic.gdx.math.vector3 :as vector3]
            [com.badlogic.gdx.utils.align :as align]
            [gdl.utils.viewport.fit-viewport :as fit-viewport])
  (:import (clojure.lang ILookup)
           (com.badlogic.gdx ApplicationListener
                             Audio
                             Files
                             Gdx
                             Graphics)
           (com.badlogic.gdx.audio Sound)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application
                                             Lwjgl3ApplicationConfiguration)
           (com.badlogic.gdx.files FileHandle)
           (com.badlogic.gdx.graphics Colors
                                      GL20
                                      Pixmap
                                      Pixmap$Format
                                      Texture
                                      OrthographicCamera)
           (com.badlogic.gdx.graphics.g2d Batch
                                          BitmapFont
                                          TextureRegion
                                          SpriteBatch)
           (com.badlogic.gdx.graphics.g2d.freetype FreeTypeFontGenerator
                                                   FreeTypeFontGenerator$FreeTypeFontParameter)
           (com.badlogic.gdx.utils Disposable)
           (org.lwjgl.system Configuration)))

;;;;; Helpers

(defn- text-height [^BitmapFont font text]
  (-> text
      (str/split #"\n")
      count
      (* (.getLineHeight font))))

(defn- create-parameter
  [{:keys [size
           min-filter
           mag-filter]}]
  (let [params (FreeTypeFontGenerator$FreeTypeFontParameter.)]
    (set! (.size params) size)
    (set! (.minFilter params) min-filter)
    (set! (.magFilter params) mag-filter)
    params))

(defn- configure!
  [^BitmapFont font
   {:keys [scale
           enable-markup?
           use-integer-positions?]}]
  (.setScale (.getData font) scale)
  (set! (.markupEnabled (.getData font)) enable-markup?)
  (.setUseIntegerPositions font use-integer-positions?)
  font)

;;;;; API

(defn application [config]
  (.set Configuration/GLFW_LIBRARY_NAME "glfw_async")
  (Lwjgl3Application. (reify ApplicationListener
                        (create [_]
                          ((:create config) {:ctx/audio    Gdx/audio
                                             :ctx/files    Gdx/files
                                             :ctx/graphics Gdx/graphics
                                             :ctx/input    Gdx/input}))
                        (dispose [_]
                          ((:dispose config)))
                        (render [_]
                          ((:render config)))
                        (resize [_ width height]
                          ((:resize config) width height))
                        (pause [_])
                        (resume [_]))
                      (doto (Lwjgl3ApplicationConfiguration.)
                        (.setWindowedMode (:width (:windowed-mode config))
                                          (:height (:windowed-mode config)))
                        (.setTitle (:title config))
                        (.setForegroundFPS (:foreground-fps config)))))

(defn post-runnable! [f]
  (.postRunnable Gdx/app f))

(defn def-colors [colors]
  (doseq [[name color-params] colors]
    (Colors/put name (color/->java color-params))))

(defn freetype-font
  [file-handle
   {:keys [size
           quality-scaling
           enable-markup?
           use-integer-positions?]}]
  (let [generator (FreeTypeFontGenerator. file-handle)
        font (.generateFont generator (create-parameter {:size (* size quality-scaling)
                                                         ; :texture-filter/linear because scaling to world-units
                                                         :min-filter (texture-filter/k->value :linear)
                                                         :mag-filter (texture-filter/k->value :linear)}))]
    (configure! font {:scale (/ quality-scaling)
                      :enable-markup? enable-markup?
                      :use-integer-positions? use-integer-positions?})))

(defn orthographic-camera
  ([]
   (proxy [OrthographicCamera ILookup] []
     (valAt [k]
       (let [^OrthographicCamera this this]
         (case k
           :camera/combined (.combined this)
           :camera/zoom (.zoom this)
           :camera/frustum {:frustum/plane-points (mapv vector3/clojurize (.planePoints (.frustum this)))}
           :camera/position (vector3/clojurize (.position this))
           :camera/viewport-width  (.viewportWidth  this)
           :camera/viewport-height (.viewportHeight this))))))
  ([& {:keys [y-down? world-width world-height]}]
   (doto (orthographic-camera)
     (OrthographicCamera/.setToOrtho y-down? world-width world-height))))

;;;;; extend-types

(extend-type Audio
  clojure.audio/Audio
  (new-sound [this file-handle]
    (.newSound this file-handle)))

(extend-type Sound
  clojure.audio.sound/Sound
  (play! [this]
    (.play this)))

(extend-type Files
  clojure.files/Files
  (internal [this path]
    (.internal this path)))

(extend-type FileHandle
  clojure.files.file-handle/FileHandle
  (list [this]
    (.list this))

  (directory? [this]
    (.isDirectory this))

  (extension [this]
    (.extension this))

  (path [this]
    (.path this)))

(extend-type Disposable
  clojure.disposable/Disposable
  (dispose! [this]
    (.dispose this)))

(extend-type Batch
  clojure.graphics.batch/Batch
  (draw! [this texture-region x y [w h] rotation]
    (.draw this
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

  (set-color! [this [r g b a]]
    (.setColor this r g b a))

  (set-projection-matrix! [this matrix]
    (.setProjectionMatrix this matrix))

  (begin! [this]
    (.begin this))

  (end! [this]
    (.end this)))

(extend-type BitmapFont
  clojure.graphics.bitmap-font/BitmapFont
  (draw! [font
          batch
          {:keys [scale text x y up? h-align target-width wrap?]}]
    {:pre [(or (nil? h-align)
               (contains? align/k->value h-align))]}
    (let [old-scale (.scaleX (.getData font))]
      (.setScale (.getData font) (float (* old-scale scale)))
      (.draw font
             batch
             text
             (float x)
             (float (+ y (if up? (text-height font text) 0)))
             (float target-width)
             (get align/k->value (or h-align :center))
             wrap?)
      (.setScale (.getData font) (float old-scale)))))

(extend-type TextureRegion
  clojure.graphics.texture-region/TextureRegion
  (dimensions [texture-region]
    [(.getRegionWidth  texture-region)
     (.getRegionHeight texture-region)]))

(extend-type Graphics
  clojure.graphics/Graphics
  (delta-time [this]
    (.getDeltaTime this))
  (frames-per-second [this]
    (.getFramesPerSecond this))
  (set-cursor! [this cursor]
    (.setCursor this cursor))
  (cursor [this pixmap hotspot-x hotspot-y]
    (.newCursor this pixmap hotspot-x hotspot-y))
  (clear!
    ([this [r g b a]]
     (clojure.graphics/clear! this r g b a))
    ([this r g b a]
     (let [clear-depth? false
           apply-antialiasing? false
           gl20 (.getGL20 this)]
       (GL20/.glClearColor gl20 r g b a)
       (let [mask (cond-> GL20/GL_COLOR_BUFFER_BIT
                    clear-depth? (bit-or GL20/GL_DEPTH_BUFFER_BIT)
                    (and apply-antialiasing? (.coverageSampling (.getBufferFormat this)))
                    (bit-or GL20/GL_COVERAGE_BUFFER_BIT_NV))]
         (GL20/.glClear gl20 mask)))))
  (texture [_ file-handle]
    (Texture. ^FileHandle file-handle))
  (pixmap
    ([_ ^FileHandle file-handle]
     (Pixmap. file-handle))
    ([_ width height pixmap-format]
     (Pixmap. (int width)
              (int height)
              (case pixmap-format
                :pixmap.format/RGBA8888 Pixmap$Format/RGBA8888))))

  (fit-viewport [_ width height camera]
    (fit-viewport/create width height camera))

  (sprite-batch [_]
    (SpriteBatch.)))

(extend-type OrthographicCamera
  clojure.graphics.orthographic-camera/OrthographicCamera
  (set-position! [this [x y]]
    (set! (.x (.position this)) (float x))
    (set! (.y (.position this)) (float y))
    (.update this))

  (set-zoom! [this amount]
    (set! (.zoom this) amount)
    (.update this)))
