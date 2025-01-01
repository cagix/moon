(ns ^:no-doc anvil.effect.target.audiovisual
  (:require [cdq.context :as world]
            [clojure.component :as component :refer [defcomponent]]))

(defcomponent :effects.target/audiovisual
  (component/applicable? [_ {:keys [effect/target]}]
    target)

  (component/useful? [_ _ _c]
    false)

  (component/handle [[_ audiovisual] {:keys [effect/target]} c]
    (world/audiovisual c
                       (:position @target)
                       audiovisual)))
