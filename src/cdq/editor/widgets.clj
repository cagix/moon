(ns cdq.editor.widgets)

(def k->methods*
  '{:s/map {:create cdq.editor.widget.map/create
            :value cdq.editor.widget.map/value}
    :default {:create cdq.editor.widget.default/create
              :value cdq.editor.widget.default/value}
    :widget/edn {:create cdq.editor.widget.edn/create
                 :value cdq.editor.widget.edn/value}
    :string {:create cdq.editor.widget.string/create
             :value cdq.editor.widget.string/value}
    :boolean {:create cdq.editor.boolean/create
              :value cdq.editor.boolean/value}
    :enum {:create cdq.editor.widget.enum/create
           :value cdq.editor.widget.enum/value}
    :s/sound {:create cdq.editor.sound/create}
    :s/one-to-one {:create cdq.editor.widget.one-to-one/create
                   :value cdq.editor.widget.one-to-one/value}
    :s/one-to-many {:create cdq.editor.widget.one-to-many/create
                    :value cdq.editor.widget.one-to-many/value}
    :s/image     {:create cdq.editor.widget.image/create}
    :s/animation {:create cdq.editor.widget.animation/create}})

(require 'cdq.effects)

(def k->methods
  (cdq.effects/walk-method-map k->methods*))
