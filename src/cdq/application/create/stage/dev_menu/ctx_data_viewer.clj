(ns cdq.application.create.stage.dev-menu.ctx-data-viewer
  (:require [cdq.ui.widget :as widget]
            [com.badlogic.gdx.scenes.scene2d.stage :as stage]))

(def menu
  {:label "Ctx Data"
   :items [{:label "Show data"
            :on-click (fn [_actor {:keys [ctx/stage] :as ctx}]
                        (stage/add! stage (widget/data-viewer
                                           {:title "Context"
                                            :data ctx
                                            :width 500
                                            :height 500})))}]})
