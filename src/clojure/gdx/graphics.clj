(ns clojure.gdx.graphics
  (:import (com.badlogic.gdx Graphics)
           (com.badlogic.gdx.graphics GL20)))

(defn delta-time [graphics]
  (Graphics/.getDeltaTime graphics))

(defn frames-per-second [graphics]
  (Graphics/.getFramesPerSecond graphics))

(defn set-cursor! [graphics cursor]
  (Graphics/.setCursor graphics cursor))

(defn cursor [graphics pixmap hotspot-x hotspot-y]
  (Graphics/.newCursor graphics pixmap hotspot-x hotspot-y))

(defn clear!
  [graphics r g b a]
  (let [clear-depth? false
        apply-antialiasing? false
        gl20 (Graphics/.getGL20 graphics)]
    (GL20/.glClearColor gl20 r g b a)
    (let [mask (cond-> GL20/GL_COLOR_BUFFER_BIT
                 clear-depth? (bit-or GL20/GL_DEPTH_BUFFER_BIT)
                 (and apply-antialiasing? (.coverageSampling (Graphics/.getBufferFormat graphics)))
                 (bit-or GL20/GL_COVERAGE_BUFFER_BIT_NV))]
      (GL20/.glClear gl20 mask))))
