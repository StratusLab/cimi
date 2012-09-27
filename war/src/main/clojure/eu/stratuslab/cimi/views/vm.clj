(ns eu.stratuslab.cimi.views.vm
  (:require [noir.core :refer [defpage]]
            [noir.response :as resp]
            [clojure.tools.logging :as log]
            [net.cgrand.enlive-html :refer [deftemplate content]]
            [eu.stratuslab.cimi.vmm.dummy :as dummy]))

;;
;; All resources are available to authenticated users only.  The
;; pre-route requiring this is defined in the authn views.  Do not add
;; additional pre-routes for authentication here.
;;

(deftemplate vm-create-form "eu/stratuslab/authn/vm_rest/views/vm-create-form.html"
  [fields]
  [:div#wrapper :p] (content "VM Launcher")
  [:input#marketplace-id] (content (:marketplace-id fields))
  [:input#cpu-cores] (content (:cpu-cores fields)))

(deftemplate vm-update-form "eu/stratuslab/authn/vm_rest/views/vm-update-form.html"
  [fields]
  [:div#wrapper :p] (content "VM Status")
  [:text#vmid] (content (:vmid fields))
  [:input#state] (content (:state fields))
  [:input#marketplace-id] (content (:marketplace-id fields))
  [:input#cpu-cores] (content (:cpu-cores fields)))

;; html page for starting machine (post to /vm)
(defpage [:get "/vm"] {:as fields}
  (apply str (vm-create-form fields)))

;; post to create new machine
(defpage [:post ["/vm"]] {:as data}
  (dummy/create data))

;; summary all of the VMs
(defpage [:get "/vm/"] {}
  (dummy/summary))

;; get the VM status (should be presented as a form if html requested)
(defpage [:get "/vm/:vmid"] {:keys [vmid]}
  (let [data (dummy/status vmid)]
    (apply str (vm-update-form data))))

;; update VM status
(defpage [:put "/vm/:vmid"] {:as data}
  (dummy/update data))

;; kill a VM
(defpage [:delete "/vm/:vmid"] {:keys [vmid]}
  (dummy/delete vmid))
