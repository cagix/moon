(ns clojure.gdx.graphics
  (:require [clojure.gdx.graphics.gl20 :as gl20])
  (:import (com.badlogic.gdx Graphics)))

(defn gl20 [^Graphics graphics]
  (.getGL20 graphics))

(defn buffer-format [^Graphics graphics]
  (.getBufferFormat graphics))

(defn clear! [^Graphics graphics [r g b a]]
  (let [clear-depth? false
        apply-antialiasing? false
        gl (gl20 graphics)]
    (gl20/clear-color! gl r g b a)
    (let [mask (cond-> gl20/color-buffer-bit
                 clear-depth? (bit-or gl20/depth-buffer-bit)
                 (and apply-antialiasing? (.coverageSampling (buffer-format graphics)))
                 (bit-or gl20/coverage-buffer-bit-nv))]
      (gl20/clear! gl mask))))

(defn set-cursor! [^Graphics graphics cursor]
  (.setCursor graphics cursor))

(defn frames-per-second [^Graphics graphics]
  (.getFramesPerSecond graphics))

(defn delta-time [^Graphics graphics]
  (.getDeltaTime graphics))
