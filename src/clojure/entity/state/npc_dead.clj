(ns clojure.entity.state.npc-dead
  (:require [clojure.state :as state]
            [clojure.utils :refer [defcomponent]]))

(defcomponent :npc-dead
  (state/enter! [_ eid]
    [[:tx/mark-destroyed eid]]))
