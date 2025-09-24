(ns cdq.application.create.ui.dev-menu.help-info-text)

(def ^:private help-str
  "[W][A][S][D] - Move\n[I] - Inventory window\n[E] - Entity Info window\n[-]/[=] - Zoom\n[P]/[SPACE] - Unpause")

(def menu
  {:label "Help"
   :items [{:label help-str}]})
