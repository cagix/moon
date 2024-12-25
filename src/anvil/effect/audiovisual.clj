(ns ^:no-doc anvil.effect.audiovisual
  (:require [anvil.component :as component]
            [anvil.world :as c]))

(defmethods :effects/audiovisual
  (component/applicable? [_ {:keys [effect/target-position]}]
    target-position)

  (component/useful? [_ _]
    false)

  (component/handle [[_ audiovisual] {:keys [effect/target-position]}]
    (c/audiovisual target-position audiovisual)))
