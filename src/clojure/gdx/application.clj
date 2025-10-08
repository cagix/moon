(ns clojure.gdx.application
  (:require [clojure.gdx :as gdx]
            [clojure.gdx.audio :as audio])
  (:import (com.badlogic.gdx ApplicationListener
                             Audio
                             Files
                             Gdx
                             Graphics)
           (com.badlogic.gdx.audio Sound)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application
                                             Lwjgl3ApplicationConfiguration)
           (com.badlogic.gdx.graphics Color
                                      Colors
                                      Pixmap
                                      Texture$TextureFilter)
           (com.badlogic.gdx.graphics.g2d SpriteBatch)
           (com.badlogic.gdx.graphics.g2d.freetype FreeTypeFontGenerator
                                                   FreeTypeFontGenerator$FreeTypeFontParameter)
           (org.lwjgl.system Configuration)
           (space.earlygrey.shapedrawer ShapeDrawer)))

(extend-type Sound
  audio/Sound
  (play! [this]
    (.play this))
  (dispose! [this]
    (.dispose this)))

(defrecord Context [^Audio audio
                    ^Files files
                    ^Graphics graphics
                    input]
  gdx/Audio
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
  )

(defn start!
  [{:keys [title
           window
           fps
           create!
           dispose!
           render!
           resize!
           colors]}]
  (doseq [[name [r g b a]] colors]
    (Colors/put name (Color. r g b a)))
  (.set Configuration/GLFW_LIBRARY_NAME "glfw_async")
  (Lwjgl3Application. (reify ApplicationListener
                        (create [_]
                          (create! (map->Context
                                    {:audio    Gdx/audio
                                     :files    Gdx/files
                                     :graphics Gdx/graphics
                                     :input    Gdx/input})))

                        (dispose [_]
                          (dispose!))

                        (render [_]
                          (render!))

                        (resize [_ width height]
                          (resize! width height))

                        (pause [_])

                        (resume [_]))
                      (doto (Lwjgl3ApplicationConfiguration.)
                        (.setTitle title)
                        (.setWindowedMode (:width window)
                                          (:height window))
                        (.setForegroundFPS fps))))
