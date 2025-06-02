(ns cdq.entity.state.npc-dead
  (:require [cdq.state :as state]
            [clojure.utils :refer [defcomponent]]))

(defcomponent :npc-dead
  (state/enter! [_ eid]
    [[:tx/mark-destroyed eid]]))
