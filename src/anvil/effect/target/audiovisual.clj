(ns ^:no-doc anvil.effect.target.audiovisual
  (:require [anvil.component :as component]
            [anvil.world :as world]))

(defmethods :effects.target/audiovisual
  (component/applicable? [_ {:keys [effect/target]}]
    target)

  (component/useful? [_ _]
    false)

  (component/handle [[_ audiovisual] {:keys [effect/target]} c]
    (world/audiovisual c
                       (:position @target)
                       audiovisual)))
