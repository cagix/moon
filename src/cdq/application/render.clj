(ns cdq.application.render
  (:require cdq.utils))

(def render-fns
  '[(cdq.content-grid/assoc-active-entities)
    (cdq.render.camera/set-on-player)
    (cdq.gdx.graphics/clear-screen)
    (cdq.render.tiled-map/draw)
    (cdq.graphics/draw-on-world-view [(cdq.render.before-entities/render)
                                      (cdq.world.graphics/render-entities
                                       {:below {:entity/mouseover? cdq.world.graphics/draw-faction-ellipse
                                                :player-item-on-cursor cdq.world.graphics/draw-world-item-if-exists
                                                :stunned cdq.world.graphics/draw-stunned-circle}
                                        :default {:entity/image cdq.world.graphics/draw-image-as-of-body
                                                  :entity/clickable cdq.world.graphics/draw-text-when-mouseover-and-text
                                                  :entity/line-render cdq.world.graphics/draw-line}
                                        :above {:npc-sleeping cdq.world.graphics/draw-zzzz
                                                :entity/string-effect cdq.world.graphics/draw-text
                                                :entity/temp-modifier cdq.world.graphics/draw-filled-circle-grey}
                                        :info {:entity/hp cdq.world.graphics/draw-hpbar-when-mouseover-and-not-full
                                               :active-skill cdq.world.graphics/draw-skill-image-and-active-effect}})
                                      (cdq.render.after-entities/render)])
    (cdq.render/draw-stage)
    (cdq.render/update-stage)
    (cdq.render/player-state-input)
    (cdq.render/update-mouseover-entity)
    (cdq.render/update-paused)
    (cdq.render/when-not-paused)
    (cdq.render/remove-destroyed-entities)
    (cdq.render/camera-controls)
    (cdq.render/window-controls)])

(defn context [context]
  (reduce (fn [context fn-invoc]
            (cdq.utils/req-resolve-call fn-invoc context))
          context
          render-fns))
