/*******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/		
package org.jboss.tools.aerogear.hybrid.core.plugin;
		

import java.io.File;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jboss.tools.aerogear.hybrid.core.HybridCore;
import org.jboss.tools.aerogear.hybrid.core.platform.IPluginInstallationAction;
import org.jboss.tools.aerogear.hybrid.core.util.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XMLConfigFileAction implements IPluginInstallationAction {
	
	protected final File target;
	protected final String parent;
	protected final String xml;
	private XPathExpression xpathExpression;
	
	public XMLConfigFileAction(File target, String parent, String xml){
		this.target = target;
		this.parent = parent;
		this.xml = xml;
	}

	@Override
	public void install() throws CoreException {
		Document doc = XMLUtil.loadXML(target);
		Document newNode = XMLUtil.loadXML(xml);//config-file node
		Node node = getParentNode(doc.getDocumentElement());
		if(node == null ){
			throw createParentNodeException();
		}
		NodeList childNodes = newNode.getDocumentElement().getChildNodes(); //append child nodes of config-file
		for(int i = 0; i < childNodes.getLength(); i++ ){
			Node importedNode = doc.importNode(childNodes.item(i), true);
			node.appendChild(importedNode);
		}
		XMLUtil.saveXML(target, doc);
	}
	
	@Override
	public void unInstall() throws CoreException {
		Document doc = XMLUtil.loadXML(target, false);//Namespaces cause the Node.isEqualNode to fail
		Document node = XMLUtil.loadXML(xml);         //because snippets usually can not be namespaces aware
		Node parentNode = getParentNode(doc.getDocumentElement());
		if(parentNode == null ){
			throw createParentNodeException();
		}
		NodeList childNodes = node.getDocumentElement().getChildNodes(); 
		for(int i = 0; i < childNodes.getLength(); i++ ){
			Node importedNode = doc.importNode(childNodes.item(i),true);
			NodeList children = parentNode.getChildNodes();
			for (int j = 0; j < children.getLength(); j++) {
				Node item = children.item(j);
				if(item.isEqualNode(importedNode)){
					parentNode.removeChild(item);
					break;
				}
			}
			
		}
		XMLUtil.saveXML(target, doc);
	}

	private CoreException createParentNodeException() {
		return new CoreException(new Status(IStatus.ERROR, HybridCore.PLUGIN_ID,
				"Parent node could not be retrieved on "+target.getName()+ " with expression " + parent));
	}

	private XPathExpression getXPathExpression() throws XPathExpressionException {
		if(xpathExpression == null ){
			XPath xpath = XPathFactory.newInstance().newXPath();
			xpathExpression = xpath.compile(parent);
		}
		return xpathExpression;
	}
	
	private Node getParentNode(Node root) throws CoreException {
		try {
			return (Node) getXPathExpression().evaluate(root,
					XPathConstants.NODE);
		} catch (XPathExpressionException e) {
			throw new CoreException(new Status(IStatus.ERROR,
					HybridCore.PLUGIN_ID, "Error getting the parent node for " + target.getName()
							+ " with xpath expression "+ parent, e));
		}
	}
	

}
