# Konstellate

Visualize Kubernetes Applications

![image](https://user-images.githubusercontent.com/3777243/57480607-37d22a00-726e-11e9-975f-131989250a22.png)

## What is it?

Konstellate is a UI to create edit and manage Kubernetes resources and their relationships. You can easily create complex YAML and export them as Helm charts or Kustomize templates

Try out the alpha build - [Konstellate Demo](https://containership.github.io/konstellate)

You can create resources from one of the templates using the editor, or import yaml that you already have.

![konstellate-editor](https://user-images.githubusercontent.com/3777243/57794354-6ee28880-7711-11e9-8f0c-940ec8004788.gif)

Once you have a few resources created, you can simply drag a line between the two to connect them. Konstellate will look for any possible ways they can be connected and give you a drop down of the options.

![konstellate-connect](https://user-images.githubusercontent.com/3777243/57794379-81f55880-7711-11e9-8a28-52b9af888fac.gif)

Once your application is created you can clone it in a new workspace and make additional changes to it there. These changes will be reflected as variables in the helm chart or overlays kustomize templates as you export it.

![konstellate-kustomize](https://user-images.githubusercontent.com/3777243/57794412-96d1ec00-7711-11e9-8796-7dddefa30532.gif)


## Known Issues

* Warn users if no connection types are available
* Implement Export YAML
* Implement Import Helm + Kustomize
* Update Add/Remove buttons on editor

## Future Roadmap

* Auto populate required fields in resource templates
* Add tree view into YAML spec
* Packaging as electron/docker image + local file system sync
* Tie Konstellate into running clusters + kubectl plugin
* Enable GitOps flow

## License
[MIT](https://choosealicense.com/licenses/mit/)
