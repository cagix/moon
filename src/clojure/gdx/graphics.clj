(ns clojure.gdx.graphics
  (:import (com.badlogic.gdx Graphics)
           (com.badlogic.gdx.graphics GL20)))

(defn clear! [^Graphics graphics [r g b a]]
  (let [clear-depth? false
        apply-antialiasing? false
        gl20 (.getGL20 graphics)]
    (GL20/.glClearColor gl20 r g b a)
    (let [mask (cond-> GL20/GL_COLOR_BUFFER_BIT
                 clear-depth? (bit-or GL20/GL_DEPTH_BUFFER_BIT)
                 (and apply-antialiasing? (.coverageSampling (.getBufferFormat graphics)))
                 (bit-or GL20/GL_COVERAGE_BUFFER_BIT_NV))]
      (GL20/.glClear gl20 mask))))
