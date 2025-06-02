(ns clojure.entity.mana
  (:require [clojure.entity :as entity]
            [clojure.utils :refer [defcomponent]]))

(defcomponent :entity/mana
  (entity/create [[_ v] _ctx]
    [v v]))
