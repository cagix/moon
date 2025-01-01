(ns ^:no-doc anvil.entity.projectile-collision
  (:require [clojure.component :as component :refer [defcomponent]]))

(defcomponent :entity/projectile-collision
  (component/create [[_ v] c]
    (assoc v :already-hit-bodies #{})))
