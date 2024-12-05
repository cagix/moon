(ns forge.app
  (:require [clojure.gdx :as gdx]
            [forge.core :refer [defsystem]]))

(def exit gdx/exit)

(defmacro post-runnable [& exprs]
  `(gdx/post-runnable (fn [] ~@exprs)))

(defsystem create)
(defmethod create :default [_])

(defsystem dispose)
(defmethod dispose :default [_])

(defsystem render)
(defmethod render :default [_])

(defsystem resize)
(defmethod resize :default [_ w h])
