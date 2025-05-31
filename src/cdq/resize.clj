(ns cdq.resize)

(defprotocol Resizable
  (resize! [_ width height]))

(defn do! [{:keys [ctx/ui-viewport
                   ctx/world-viewport]}
           width
           height]
  (resize! ui-viewport    width height)
  (resize! world-viewport width height))
