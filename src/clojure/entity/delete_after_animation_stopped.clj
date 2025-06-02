(ns clojure.entity.delete-after-animation-stopped
  (:require [clojure.animation :as animation]
            [clojure.entity :as entity]
            [clojure.utils :refer [defcomponent]]))

(defcomponent :entity/delete-after-animation-stopped?
  (entity/create! [_ eid _ctx]
    (-> @eid :entity/animation :looping? not assert)
    nil)

  (entity/tick! [_ eid _ctx]
    (when (animation/stopped? (:entity/animation @eid))
      [[:tx/mark-destroyed eid]])))

