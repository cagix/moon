(ns cdq.ui.dev-menu.menus.help)

(defn create [infotext]
  {:label "Help"
   :items [{:label infotext}]})
