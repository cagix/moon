(ns cdq.start.render-layers
  (:require [cdq.walk :as walk]))

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

(defn do! [ctx]
  (assoc ctx :ctx/render-layers (walk/require-resolve-symbols render-layers)))
