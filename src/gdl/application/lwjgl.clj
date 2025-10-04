(ns gdl.application.lwjgl
  (:require [clojure.string :as str]
            [com.badlogic.gdx.utils.align :as align]
            gdl.audio
            gdl.audio.sound
            gdl.files
            gdl.files.file-handle
            gdl.graphics
            gdl.graphics.batch
            gdl.graphics.bitmap-font
            gdl.disposable)
  (:import (com.badlogic.gdx ApplicationListener
                             Audio
                             Files
                             Gdx
                             Graphics)
           (com.badlogic.gdx.audio Sound)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application
                                             Lwjgl3ApplicationConfiguration)
           (com.badlogic.gdx.files FileHandle)
           (com.badlogic.gdx.graphics GL20
                                      Texture
                                      Pixmap
                                      Pixmap$Format)
           (com.badlogic.gdx.graphics.g2d BitmapFont
                                          SpriteBatch)
           (com.badlogic.gdx.utils Disposable)
           (org.lwjgl.system Configuration)))

(defprotocol Listener
  (create [_ context])
  (dispose [_])
  (render [_])
  (resize [_ width height])
  (pause [_])
  (resume [_]))

(defn start! [listener config]
  (.set Configuration/GLFW_LIBRARY_NAME "glfw_async")
  (Lwjgl3Application. (reify ApplicationListener
                        (create [_]
                          (let [state {:ctx/audio    Gdx/audio
                                       :ctx/files    Gdx/files
                                       :ctx/graphics Gdx/graphics
                                       :ctx/input    Gdx/input}]
                            (create listener state)))
                        (dispose [_]
                          (dispose listener))
                        (render [_]
                          (render listener))
                        (resize [_ width height]
                          (resize listener width height))
                        (pause [_]
                          (pause listener))
                        (resume [_]
                          (resume listener)))
                      (doto (Lwjgl3ApplicationConfiguration.)
                        (.setWindowedMode (:width (:windowed-mode config))
                                          (:height (:windowed-mode config)))
                        (.setTitle (:title config))
                        (.setForegroundFPS (:foreground-fps config)))))


(extend Audio
  gdl.audio/Audio
  {:new-sound Audio/.newSound})

(extend Sound
  gdl.audio.sound/Sound
  {:play! Sound/.play})

(extend Files
  gdl.files/Files
  {:internal Files/.internal})

(extend FileHandle
  gdl.files.file-handle/FileHandle
  {:list       FileHandle/.list
   :directory? FileHandle/.isDirectory
   :extension  FileHandle/.extension
   :path       FileHandle/.path})

(extend Disposable
  gdl.disposable/Disposable
  {:dispose! Disposable/.dispose})

(extend-type Graphics
  gdl.graphics/Graphics
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
     (gdl.graphics/clear! this r g b a))

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

  (sprite-batch [_]
    (SpriteBatch.)))

(extend-type SpriteBatch
  gdl.graphics.batch/Batch
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

(let [text-height (fn [^BitmapFont font text]
                    (-> text
                        (str/split #"\n")
                        count
                        (* (.getLineHeight font))))]
  (extend-type BitmapFont
    gdl.graphics.bitmap-font/BitmapFont
    (configure! [font {:keys [scale enable-markup?  use-integer-positions?]}]
      (.setScale (.getData font) scale)
      (set! (.markupEnabled (.getData font)) enable-markup?)
      (.setUseIntegerPositions font use-integer-positions?)
      font)

    (draw! [font batch {:keys [scale text x y up? h-align target-width wrap?]}]
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
        (.setScale (.getData font) (float old-scale))))))
