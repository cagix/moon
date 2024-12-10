(ns anvil.app
  (:require [clojure.gdx.app :as app]))

(def exit app/exit)

(defmacro post-runnable [& exprs]
  `(app/post-runnable (fn [] ~@exprs)))

(declare gui-viewport-width
         gui-viewport-height
         gui-viewport
         world-unit-scale
         world-viewport-width
         world-viewport-height
         world-viewport
         cached-map-renderer)
