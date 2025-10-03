(ns cdq.ctx.create.stage
  (:require [cdq.graphics.draws :as draws]
            [com.badlogic.gdx.scenes.scene2d.ctx :as ctx]
            [com.badlogic.gdx.scenes.scene2d.stage :as stage]))

(defn create-windows [ctx actor-fns]
  {:actor/type :actor.type/group
   :actor/name "cdq.ui.windows"
   :group/actors (for [[actor-fn & params] actor-fns]
                   (apply (requiring-resolve actor-fn) ctx params))})

(defn do!
  [{:keys [ctx/graphics]
    :as ctx}
   actor-fns]
  (extend-type (class ctx)
    ctx/Graphics
    (draw! [{:keys [ctx/graphics]} draws]
      (draws/handle! graphics draws)))
  (assoc ctx :ctx/stage (stage/create (:graphics/ui-viewport graphics)
                                      (:graphics/batch       graphics))
         :ctx/actor-fns
         (map #(update % 0 requiring-resolve) actor-fns)))
