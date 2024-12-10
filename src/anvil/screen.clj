(ns anvil.screen
  (:require [clojure.utils :refer [defsystem]]))

(defsystem enter)
(defmethod enter :default [_])

(defsystem exit)
(defmethod exit :default [_])

(declare screens
         current-k)

(defn current []
  (and (bound? #'current-k)
       (current-k screens)))

(defn change
  "Calls `exit` on the current-screen and `enter` on the new screen."
  [new-k]
  (when-let [screen (current)]
    (exit screen))
  (let [screen (new-k screens)]
    (assert screen (str "Cannot find screen with key: " new-k))
    (def current-k new-k)
    (enter screen)))

(defn setup [screens first-k]
  (def screens screens)
  (change first-k))

(defsystem dispose)
(defmethod dispose :default [_])

(defn dispose-all []
  (run! dispose (vals screens)))

(defsystem render)
(defmethod render :default [_])

(defn render-current []
  (render (current)))
