(ns anvil.entity.hp
  (:require [anvil.entity :as entity]
            [gdl.info :as info]
            [clojure.component :as component :refer [defcomponent]]))

(defcomponent :entity/hp
  (component/info [_ _c]
    (str "Hitpoints: " (entity/hitpoints info/*info-text-entity*)))

  (component/create [[_ v] c]
    [v v]))
