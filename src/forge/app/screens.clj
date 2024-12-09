(ns forge.app.screens
  (:require [anvil.app :as app]
            [anvil.screen :as screen]
            [clojure.utils :refer [bind-root mapvals]]))

(defn create [[_ {:keys [ks first-k]}]]
  (bind-root app/screens (mapvals
                          (fn [ns-sym]
                            (require ns-sym)
                            ((ns-resolve ns-sym 'create)))
                          ks))
  (app/change-screen first-k))

(defn destroy [_]
  (run! screen/dispose (vals app/screens)))

(defn render [_]
  (screen/render (app/current-screen)))
