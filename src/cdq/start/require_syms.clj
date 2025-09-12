(ns cdq.start.require-syms
  (:require [clojure.walk :as walk]))

(def render-layers
  '[{:entity/mouseover? cdq.entity.mouseover/draw
     :stunned cdq.entity.state.stunned/draw
     :player-item-on-cursor cdq.entity.state.player-item-on-cursor/draw}
    {:entity/clickable cdq.entity.clickable/draw
     :entity/animation cdq.entity.animation/draw
     :entity/image cdq.entity.image/draw
     :entity/line-render cdq.entity.line-render/draw}
    {:npc-sleeping cdq.entity.state.npc-sleeping/draw
     :entity/temp-modifier cdq.entity.temp-modifier/draw
     :entity/string-effect cdq.entity.string-effect/draw}
    {:creature/stats cdq.entity.stats/draw
     :active-skill cdq.entity.state.active-skill/draw}])

(defn require-resolve-symbols [form]
  (walk/postwalk (fn [form]
                   (if (symbol? form)
                     (let [var (requiring-resolve form)]
                       (assert var form)
                       var)
                     form))
                 form))

(defn do! [ctx]
  (-> ctx
      (update :ctx/entity-components require-resolve-symbols)
      (update :ctx/entity-states     require-resolve-symbols)
      (update :ctx/info              require-resolve-symbols)
      (update :ctx/draw-fns          require-resolve-symbols)
      (assoc :ctx/render-layers     (require-resolve-symbols render-layers))))
