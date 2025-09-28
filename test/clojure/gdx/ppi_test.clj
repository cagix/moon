; com.badlogic.gdx.tests.DpiTest
(ns clojure.gdx.ppi-test
  (:require [gdl.application :as application])
  (:import (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.graphics GL20)
           com.badlogic.gdx.graphics.g2d.BitmapFont
           com.badlogic.gdx.graphics.g2d.SpriteBatch))

(def state (atom nil))

(defn- info-str [graphics]
  (str
   "Density: " (.getDensity graphics) "\n" "PPC-x: "  (.getPpcX graphics) "\n" "PPC-y: "
    (.getPpcY graphics)  "\n"  "PPI-x: "  (.getPpiX graphics)  "\n"  "PPI-y: "  (.getPpiY graphics)))

(defn -main []
  (application/start!
   {:listener (reify application/Listener
                (create [_ context]
                  (def batch (SpriteBatch.))
                  (def font (BitmapFont.)))
                (dispose [_]
                  )
                (pause [_])
                (render [_]
                  (.glClear Gdx/gl GL20/GL_COLOR_BUFFER_BIT)
                  (.begin batch)
                  (.draw font
                         batch
                         ^CharSequence (info-str Gdx/graphics)
                         (float 0)
                         (float (.getHeight Gdx/graphics)))
                  (.end batch))
                (resize [_ width height]
                  )
                (resume [_]))
    :config {:title "Fooz Baaz"
             :windowed-mode {:width 800
                             :height 600}
             :foreground-fps 60
             :mac {:glfw-async? true}}}))
