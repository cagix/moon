(ns cdq.ui.dev-menu.update-labels.paused)

(def item
  {:label "paused?"
   :update-fn (comp :world/paused? :ctx/world)})
