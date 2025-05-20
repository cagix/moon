(ns cdq.entity.mana
  (:require [cdq.entity :as entity]
            [cdq.utils :refer [defcomponent]]))

(defcomponent :entity/mana
  (entity/create [[_ v] _ctx]
    [v v]))
