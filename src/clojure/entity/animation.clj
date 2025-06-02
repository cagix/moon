(ns clojure.entity.animation
  (:require [clojure.animation :as animation]
            [clojure.entity :as entity]
            [clojure.utils :refer [defcomponent]]))

(defcomponent :entity/animation
  (entity/create! [[_ animation] eid _ctx]
    [[:tx/assoc eid :entity/image (animation/current-frame animation)]])

  (entity/tick! [[_ animation] eid _ctx]
    [[:tx/update-animation eid animation]]))
