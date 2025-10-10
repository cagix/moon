(ns clojure.gdx.graphics.gl20
  (:import (com.badlogic.gdx.graphics GL20)))

(defn clear-color! [this r g b a]
  (.glClearColor this r g b a))

(def color-buffer-bit GL20/GL_COLOR_BUFFER_BIT)
(def depth-buffer-bit GL20/GL_DEPTH_BUFFER_BIT)
(def coverage-buffer-bit-nv GL20/GL_COVERAGE_BUFFER_BIT_NV)

(defn clear! [this mask]
  (.glClear this mask))
