(ns cdq.entity.mana
  (:require [cdq.entity :as entity]
            [gdl.utils :refer [defcomponent]]))

(defcomponent :entity/mana
  (entity/create [[_ v] _ctx]
    [v v]))
