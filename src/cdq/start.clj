(ns cdq.start
  (:require cdq.start.install-entity-components
            cdq.start.install-effects
            cdq.start.install-txs
            cdq.start.install-editor-widgets
            cdq.start.set-icon
            cdq.start.gdx-app)
  (:gen-class))

(defn -main []
  (doseq [f [cdq.start.install-entity-components/do!
             cdq.start.install-effects/do!
             cdq.start.install-txs/do!
             cdq.start.install-editor-widgets/do!
             cdq.start.set-icon/do!
             cdq.start.gdx-app/do!]]
    (f)))
