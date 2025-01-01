(ns ^:no-doc anvil.effect.target.audiovisual
  (:require [gdl.effect.component :as component]
            [cdq.context :as world]
            [clojure.component :refer [defcomponent]]))

(defcomponent :effects.target/audiovisual
  (component/applicable? [_ {:keys [effect/target]}]
    target)

  (component/useful? [_ _ _c]
    false)

  (component/handle [[_ audiovisual] {:keys [effect/target]} c]
    (world/audiovisual c
                       (:position @target)
                       audiovisual)))
