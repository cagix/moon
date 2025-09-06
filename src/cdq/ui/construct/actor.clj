(ns cdq.ui.construct.actor
  (:require [cdq.ui :as ui]
            [clojure.gdx.scenes.scene2d.actor :as actor]))

(defn create [opts]
  (doto (actor/create
         (fn [this delta]
           (when-let [f (:act opts)]
             (ui/try-act this delta f)))
         (fn [this _batch _parent-alpha]
           (when-let [f (:draw opts)]
             (ui/try-draw this f))))
    (ui/set-opts! opts)))
