(ns forge.app.screens
  (:require [clojure.utils :refer [bind-root mapvals]]
            [forge.screen :as screen]))

(declare ^:private screens
         ^:private current-screen-key)

(defn current-screen []
  (and (bound? #'current-screen-key)
       (current-screen-key screens)))

(defn change-screen
  "Calls `exit` on the current-screen and `enter` on the new screen."
  [new-k]
  (when-let [screen (current-screen)]
    (screen/exit screen))
  (let [screen (new-k screens)]
    (assert screen (str "Cannot find screen with key: " new-k))
    (bind-root current-screen-key new-k)
    (screen/enter screen)))

(defn create [[_ {:keys [ks first-k]}]]
  (bind-root screens (mapvals
                      (fn [ns-sym]
                        (require ns-sym)
                        ((ns-resolve ns-sym 'create)))
                      ks))
  (change-screen first-k))

(defn destroy [_]
  (run! screen/dispose (vals screens)))

(defn render [_]
  (screen/render (current-screen)))
