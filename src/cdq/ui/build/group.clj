(ns cdq.ui.build.group
  (:require [clojure.gdx.scene2d.group :as group]
            [cdq.ui.build.actor :as actor]))

(defn set-opts! [group opts]
  (run! (fn [actor]
          (group/add-actor! group actor))
        (:group/actors opts))
  (actor/set-opts! group opts))

(defn create [opts]
  (doto (group/create)
    (set-opts! opts)))
