(ns anvil.screen
  (:require [clojure.component :refer [defsystem]]
            [clojure.gdx.scene2d.stage :as stage]))

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

(defsystem actors)
(defmethod actors :default [_])

(defn setup [gui-viewport batch {:keys [screens first-k]}]
  (def screens
    (into {}
          (for [k screens]
            [k [:screens/stage {:stage (stage/create gui-viewport
                                                     batch
                                                     (actors [k]))
                                :sub-screen [k]}]])))
  (change first-k))

(defsystem dispose)
(defmethod dispose :default [_])

(defn dispose-all []
  (run! dispose (vals screens)))

(defsystem render)
(defmethod render :default [_])

(defn render-current []
  (render (current)))
