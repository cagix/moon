(ns ^:no-doc anvil.effect.audiovisual
  (:require [cdq.context :as c]
            [clojure.component :as component :refer [defcomponent]]))

(defcomponent :effects/audiovisual
  (component/applicable? [_ {:keys [effect/target-position]}]
    target-position)

  (component/useful? [_ _ _c]
    false)

  (component/handle [[_ audiovisual] {:keys [effect/target-position]} c]
    (c/audiovisual c target-position audiovisual)))
