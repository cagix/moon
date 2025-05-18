(ns cdq.ui.editor.widget.image
  (:require #_[cdq.ctx :as ctx]
            [cdq.schema :as schema]
            [cdq.ui.editor.widget :as widget]
            #_[gdl.assets :as assets]
            [gdl.ui :as ui]))

; too many ! too big ! scroll ... only show files first & preview?
; make tree view from folders, etc. .. !! all creatures animations showing...
#_(defn- texture-rows []
    (for [file (sort (assets/all-of-type ctx/assets :texture))]
      [(ui/image-button (image file) (fn []))]
      #_[(ui/text-button file (fn []))]))

(defmethod widget/create :s/image [schema image]
  (ui/image-button (schema/edn->value schema image)
                   (fn on-clicked [])
                   {:scale 2})
  #_(ui/image-button image
                     #(ui/add! ctx/stage (scroll-pane/choose-window (texture-rows)))
                     {:dimensions [96 96]})) ; x2  , not hardcoded here
