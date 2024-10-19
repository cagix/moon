(ns world.entity.modifiers
  (:require [component.core :refer [defc]]
            [component.info :as info]
            [component.tx :as tx]
            [data.modifiers :as mods]
            [world.entity :as entity]))

; specs here ???

; mods one of existing :modifier/foo with {ops and value] - not coll

(defn update-mods [[_ eid mods] f]
  [[:e/update eid :entity/modifiers #(f % mods)]])

(defc :tx/apply-modifiers   (tx/handle [this] (update-mods this mods/add)))
(defc :tx/reverse-modifiers (tx/handle [this] (update-mods this mods/remove)))

(defc :entity/modifiers
  {:schema [:s/components-ns :modifier]
   :let modifiers}
  (entity/->v [_]
    (mods/coll-mods modifiers))

  (info/text [_]
    (mods/info-text modifiers)))

(defn ->modified-value [{:keys [entity/modifiers]} modifier-k base-value]
  {:pre [(= "modifier" (namespace modifier-k))]}
  (mods/apply-ops (:modifier-k modifiers)
                  base-value))
