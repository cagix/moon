(ns cdq.ui.dev-menu.menus.ctx-data-view
  (:require [cdq.ui.data-viewer :as data-view]
            [cdq.ui.stage :as stage]))

(defn items [_ctx _params]
  [{:label "Show data"
    :on-click (fn [_actor {:keys [ctx/stage] :as ctx}]
                (stage/add! stage (data-view/create {:title "Context"
                                                     :data ctx
                                                     :width 500
                                                     :height 500})))}])
